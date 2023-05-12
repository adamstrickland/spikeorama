# rt-cdc-to-fact-enrichment

Simple demonstration of using Flink as a data stream enrichment mechanism.

Uses a Debezium CDC stream as the source, and outputs protobufs to another
topic.

The [`monolith`](./opt/monolith) is a Djano project, with one application,
[`wildapp`](./opt/monolith/wildapp). It contains a simple 3-table
[schema definition](./opt/monolith/wildapp/migrations/0001_initial.py), which
is managed by a set of 3 simple [models](./opt/monolith/wildapp/models.py).

The database is Postgres, run in a container via Docker Compose, and
configured to have a [logical wal](./docker-compose.yml#L62).

There is a Debezium instance, also in a container, configured to run a
[CDC connector](./src/main/dbz/cdc-connector.json).

Flink is configured to run a job that watches the CDC topic for one of the
tables (`monolith.public.wildapp_primary`), transforms it to a Protobuf-managed
object, and then serializes the object to a "fact" topic (`wildapp.primary.facts`).

The key code to look at, i.e. what does all the work inside of Flink, is the
[StaticTransformJob](https://github.com/adamstrickland/spikeorama/blob/fact-spike/spikes/rt-cdc-to-fact-enrichment/opt/abstractfactory/src/main/java/abstractfactory/StaticTransformJob.java).

```mermaid
sequenceDiagram
  actor ds as django-seed
  participant p as models.Primary
  participant wp as wildapp_primary
  participant dbz as debezium
  participant cdct as monolith.public.wildapp_primary
  participant f as flink
  participant fctt as wildapp.primary.facts

  activate dbz
  activate wp
  activate cdct
  activate f
  activate fctt

  par
    dbz ->> wp: subscribe()
  and
    f ->> cdct: subscribe()
  end

  ds ->>+ p: create(10)
  p ->> wp: INSERT

  par
    wp -->> p: 10
    p -->> ds: OK
  and
    wp -)+ dbz: notify()
    loop 10 times
      dbz ->> wp: read()
      wp -->> dbz: wal-data
      dbz ->>+ dbz: to_cdc_json(wal-data)
      dbz -->>- dbz: cdc-json
      dbz ->> cdct: write(cdc-json)

      par
        cdct -->> dbz: OK
      and
        cdct -)+ f: notify()
        f ->> cdct: read()
        cdct -->> f: cdc-json
        f ->>+ f: map(cdc-json)
        f -->>- f: protobuf
        f -) fctt: write(protobuf)
        deactivate f
      end
    end
  end

  deactivate dbz
  deactivate dbz
  deactivate wp
  deactivate cdct
  deactivate f
  deactivate fctt
```

## Usage

### Bootstrapping

1. Start Docker Compose.

    ```sh
    docker compose up -d
    ```

    Note that it can take some time for these to come up; you may need to restart
    Debezium (for instance) *after* the broker has fully initialized.

1. Set up the database and the Django project.

    ```sh
    cd opt/monolith
    poetry install

    psql \
      postgresql://postgres:postgres@localhost:$(docker compose port postgres 5432 | cut -d: -f2)/postgres \
      -c 'create database "monolith";'

    export DATABASE_URL=postgresql://postgres:postgres@localhost:$(docker compose port postgres 5432 | cut -d: -f2)/monolith

    poetry run python manage.py makemigrations
    poetry run python manage.py migrate

    poetry run python manage.py runserver
    ```

1. Configure Debezium.

    First, confirm that Debezium is up and running:

    ```sh
    curl -XGET \
      -H"Content-Type: application/json" \
      http://$(docker compose port debezium 8083)/connectors
    ```

    The response should be an empty array (`[]`). If you get an error, double-check
    that the broker is initialized. Assuming it is, restart Debezium:

    ```sh
    docker compose restart debezium
    ```

    Now tail the logs, ...

    ```sh
    docker compose logs -f debezium
    ```

    ...and wait for it to fully initialize before configuring the connector with
    the next curl command. You're looking for the log line `Kafka Connect started`.

    ```sh
    curl -XPOST \
      -d @./src/main/dbz/cdc-connector.json \
      -H"Content-Type: application/json" \
      http://$(docker compose port debezium 8083)/connectors
    ```

    Note that there doesn't appear to be a good way for this file to be passed
    to the container and avoid all this tomfoolery. AFAIK the API needs to be
    invoked, and passed the configuration file. If you find a way to inject the
    configuration file directly, please LMK.

1. Now build the Flink job and upload it.

    ```sh
    cd opt/abstractfactory

    gradle build
    ```

    Now upload the Jar.

    ```sh
    curl -XPOST \
      -H "Expect:" \
      -F "jarfile=@build/libs/abstractfactory.jar" \
      http://$(docker compose port jobmanager 8081)/jars/upload

    ```

    Note that you may need to restart Flink.

    ```sh
    docker compose restart jobmanager
    ```

    Again, tail the log (`docker compose logs -f jobmanager`)and look for
    confirmation that Flink has *fully* restarted (it can take a minute or two).
    You're looking for a message indicating that the REST API has started; it
    should be something like `Rest endpoint listening at 0.0.0.0:8081`. On my
    distribution it was about 15 lines up from the bottom from when the logs
    stopped scrolling. Now retry the upload again, if needed.

1. Run the Flink job.

    > **NOTE: I do not have this last bit working yet via `curl`, although the
    > Proof-of-Concept does work.

    ```sh
    curl -XGET \
      http://$(docker compose port jobmanager 8081)/jars \
      | jq -r '.files[] | select(.name == "abstractfactory.jar").id' \
      | xargs -I_ \
        curl -XPOST \
        http://$(docker compose port jobmanager 8081)/jars/_/run
    ```

### Running

You may want to view messages as they flow through the topics. This is easily
done with kafkacat:

```sh
kafkacat \
  -b localhost:$(docker compose broker 9092 | cut -d: -f2) \
  -G $RANDOM \
  <topic> \
  | jq -c '.'
```

The topics you're going to want to watch are `monolith.public.wildapp_primary` and
`wildapp.primary.facts`. Alternatively, you can also tail the Docker Compose log
for the `watch` service:

```sh
docker compose logs -f watch
```

Records are added to the resulting tables via the `django-seed` tool, e.g.

```sh
cd opt/monolith
poetry run python manage seed wildapp --number=10
```

This generates the indicated `number` of each model.

You should see the appropriate number of messages in both topics, although they will
look a bit different.
