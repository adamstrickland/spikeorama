DROP PUBLICATION IF EXISTS event_publisher_outbox_pub;
DROP TABLE IF EXISTS event_publisher_outbox;

CREATE TABLE event_publisher_outbox (
  id UUID PRIMARY KEY NOT NULL,
  event_topic VARCHAR(255) NOT NULL,
  event_partition_key VARCHAR(255) NOT NULL,
  event_data JSONB NOT NULL,
  event_timestamp TIMESTAMPTZ NOT NULL
);

CREATE PUBLICATION event_publisher_outbox_pub
  FOR TABLE event_publisher_outbox;
