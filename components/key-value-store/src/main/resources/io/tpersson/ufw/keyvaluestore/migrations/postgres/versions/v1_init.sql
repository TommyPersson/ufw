--liquibase formatted sql

--changeset ufw:key-value-store-1 labels:ufw__key_value_store

CREATE TABLE ufw__key_value_store
(
    key        TEXT  NOT NULL PRIMARY KEY,
    type       INT NOT NULL,
    json       JSONB NULL,
    bytes      BYTEA NULL,
    expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version    INT
);

CREATE UNIQUE INDEX ufw__key_value_store__key_prefix
    ON ufw__key_value_store (key text_pattern_ops)