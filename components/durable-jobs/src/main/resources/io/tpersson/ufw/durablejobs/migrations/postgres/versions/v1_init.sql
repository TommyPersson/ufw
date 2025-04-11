--liquibase formatted sql

--changeset ufw:durable-jobs-1 labels:ufw__durable_jobs

CREATE TABLE ufw__periodic_jobs
(
    queue_id                              TEXT        NOT NULL,
    job_type                              TEXT        NOT NULL,
    last_scheduling_attempt               TIMESTAMPTZ NULL DEFAULT NULL,
    next_scheduling_attempt               TIMESTAMPTZ NULL DEFAULT NULL,
    last_execution_state                  INT         NULL DEFAULT NULL,
    last_execution_state_change_timestamp TIMESTAMPTZ NULL DEFAULT NULL,
    PRIMARY KEY (queue_id, job_type)
);