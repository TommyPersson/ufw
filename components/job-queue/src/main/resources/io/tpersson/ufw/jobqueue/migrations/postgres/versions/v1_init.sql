--liquibase formatted sql

--changeset ufw:job-queue-1

CREATE TABLE ufw__job_queue__jobs
(
    id TEXT NOT NULL PRIMARY KEY
);