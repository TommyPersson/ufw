--liquibase formatted sql

--changeset ufw:durable-events-1 labels:ufw

CREATE TABLE ufw__durable_events__outbox
(
    uid          SERIAL      NOT NULL PRIMARY KEY,
    id           TEXT        NOT NULL,
    topic        TEXT        NOT NULL,
    type         TEXT        NOT NULL,
    data_json    JSONB       NOT NULL,
    ce_data_json JSONB       NOT NULL,
    timestamp    TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX UX_ufw__durable_events__outbox__id
    ON ufw__durable_events__outbox (id);
