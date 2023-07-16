--liquibase formatted sql

--changeset ufw:transactional-events-1 labels:ufw__transactional_events

CREATE TABLE ufw__transactional_events__outbox
(
    uid          SERIAL      NOT NULL PRIMARY KEY,
    id           TEXT        NOT NULL,
    topic        TEXT        NOT NULL,
    type         TEXT        NOT NULL,
    data_json    JSONB       NOT NULL,
    ce_data_json JSONB       NOT NULL,
    timestamp    TIMESTAMPTZ NOT NULL
);