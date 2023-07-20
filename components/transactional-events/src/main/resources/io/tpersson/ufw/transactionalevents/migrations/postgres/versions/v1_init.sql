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

CREATE UNIQUE INDEX UX_ufw__transactional_events__outbox__id
    ON ufw__transactional_events__outbox (id);

CREATE TABLE ufw__transactional_events__queue
(
    uid                SERIAL      NOT NULL PRIMARY KEY,
    queue_id           TEXT        NOT NULL,
    id                 TEXT        NOT NULL,
    topic              TEXT        NOT NULL,
    type               TEXT        NOT NULL,
    data_json          JSONB       NOT NULL,
    ce_data_json       JSONB       NOT NULL,
    timestamp          TIMESTAMPTZ NOT NULL,
    state              INT         NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    scheduled_for      TIMESTAMPTZ NOT NULL,
    state_changed_at   TIMESTAMPTZ NOT NULL,
    watchdog_timestamp TIMESTAMPTZ NULL,
    watchdog_owner     TEXT        NULL,
    expire_at          TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX UX_ufw__transactional_events__queue__id_queue_id
    ON ufw__transactional_events__queue (queue_id, id);

-- TODO more indexes

CREATE TABLE ufw__transactional_events__failures
(
    id                TEXT        NOT NULL PRIMARY KEY,
    event_uid         BIGINT      NOT NULL,
    timestamp         TIMESTAMPTZ NOT NULL,
    error_type        TEXT        NOT NULL,
    error_message     TEXT        NOT NULL,
    error_stack_trace TEXT        NOT NULL,

    CONSTRAINT fk_event FOREIGN KEY (event_uid)
        REFERENCES ufw__transactional_events__queue (uid)
        ON DELETE CASCADE
);