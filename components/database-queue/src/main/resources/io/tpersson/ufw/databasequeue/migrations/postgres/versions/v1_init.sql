--liquibase formatted sql

--changeset ufw:database-queue-1 labels:ufw__databasequeue

CREATE TABLE ufw__db_queue__items
(
    uid                 BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id                  TEXT        NOT NULL,
    queue_id            TEXT        NOT NULL,
    type                TEXT        NOT NULL,
    state               INT         NOT NULL,
    data_json           TEXT        NOT NULL,
    metadata_json       TEXT        NOT NULL,
    concurrency_key     TEXT        NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    first_scheduled_for TIMESTAMPTZ NOT NULL,
    next_scheduled_for  TIMESTAMPTZ NOT NULL,
    state_changed_at    TIMESTAMPTZ NOT NULL,
    watchdog_timestamp  TIMESTAMPTZ NULL,
    watchdog_owner      TEXT        NULL,
    expires_at          TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX UX_ufw__db_queue__items__id ON ufw__db_queue__items (id);

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


