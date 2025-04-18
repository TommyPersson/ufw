--liquibase formatted sql

--changeset ufw:database-1 labels:ufw

CREATE TABLE ufw__database__locks
(
    id          TEXT        NOT NULL PRIMARY KEY,
    owner       TEXT        NULL,
    acquired_at TIMESTAMPTZ NULL
);