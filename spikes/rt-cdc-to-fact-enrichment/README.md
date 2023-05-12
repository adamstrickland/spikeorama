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

1. Set up the database and the Django project.

    ```sh
    cd opt/monolith
    poetry install

    psql \
      postgresql://postgres:postgres@localhost:$(docker compose port postgres 5432 | cut -d: -f2)/postgres \
      -c 'create database "wildapp";'

    export DATABASE_URL=postgresql://postgres:postgres@localhost:$(docker compose port postgres 5432 | cut -d: -f2)/wildapp

    poetry run python manage.py makemigrations
    poetry run python manage.py migrate

    poetry run python manage.py runserver
    ```

1. Configure Debezium.

    Note that there doesn't appear to be a good way for this file to be passed
    to the container; the API needs to be invoked, and passed the configuration
    file. This is done with the following incantation:

    ```sh
    curl -XPOST \
      -d @./src/main/dbz/cdc-connector.json \
      -H"Content-Type: application/json" \
      http://$(docker compose port debezium 8083)/connectors
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
