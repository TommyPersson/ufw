--liquibase formatted sql

--changeset ufw:database-queue-1 labels:ufw__database_queue

CREATE TABLE ufw__db_queue__items
(
    uid                 BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    item_id             TEXT        NOT NULL,
    queue_id            TEXT        NOT NULL,
    type                TEXT        NOT NULL,
    state               INT         NOT NULL,
    data_json           TEXT        NOT NULL,
    metadata_json       TEXT        NOT NULL,
    concurrency_key     TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    first_scheduled_for TIMESTAMPTZ NOT NULL,
    next_scheduled_for  TIMESTAMPTZ,
    state_changed_at    TIMESTAMPTZ NOT NULL,
    watchdog_timestamp  TIMESTAMPTZ,
    watchdog_owner      TEXT,
    expires_at          TIMESTAMPTZ,
    events              JSONB       NOT NULL
);

CREATE UNIQUE INDEX UX_ufw__db_queue__items__id ON ufw__db_queue__items (queue_id, item_id);

CREATE TABLE ufw__db_queue__failures
(
    id                TEXT        NOT NULL PRIMARY KEY,
    item_uid          BIGINT      NOT NULL,
    timestamp         TIMESTAMPTZ NOT NULL,
    error_type        TEXT        NOT NULL,
    error_message     TEXT        NOT NULL,
    error_stack_trace TEXT        NOT NULL,

    CONSTRAINT fk_item FOREIGN KEY (item_uid)
        REFERENCES ufw__db_queue__items (uid)
        ON DELETE CASCADE
);

-- TODO indexes

