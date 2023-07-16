--liquibase formatted sql

--changeset ufw:key-value-store-1 labels:ufw__key_value_store

CREATE TABLE ufw__key_value_store
(
    key        TEXT  NOT NULL PRIMARY KEY,
    value      JSONB NOT NULL,
    expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version    INT
);