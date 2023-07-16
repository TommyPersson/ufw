--liquibase formatted sql

--changeset ufw:aggregates-1 labels:ufw__aggregates

CREATE TABLE ufw__aggregates__facts
(
    id               UUID        NOT NULL PRIMARY KEY,
    aggregate_id     TEXT        NOT NULL,
    type             TEXT        NOT NULL,
    json             TEXT        NOT NULL,
    timestamp        TIMESTAMPTZ NOT NULL,
    version          BIGINT      NOT NULL
);

CREATE UNIQUE INDEX UX_ufw__aggregates__facts_1
    ON ufw__aggregates__facts (aggregate_id, version ASC);
