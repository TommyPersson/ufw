--liquibase formatted sql

--changeset ufw:durable-jobs-1 labels:ufw__durable_jobs

CREATE TABLE ufw__periodic_jobs
(
    queue_id                TEXT        NOT NULL,
    job_type                TEXT        NOT NULL,
    last_scheduling_attempt TIMESTAMPTZ NULL,
    next_scheduling_attempt TIMESTAMPTZ NULL,
    PRIMARY KEY (queue_id, job_type)
);