--liquibase formatted sql

--changeset ufw:job-queue-1

CREATE TABLE ufw__job_queue__jobs
(
    uid              SERIAL PRIMARY KEY,
    id               TEXT        NOT NULL,
    type             TEXT        NOT NULL,
    state            INT         NOT NULL,
    json             TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    scheduled_for    TIMESTAMPTZ NOT NULL,
    state_changed_at TIMESTAMPTZ NOT NULL,
    expire_at        TIMESTAMPTZ NULL
);

-- Primarily for getNext queries
CREATE INDEX IX_ufw__job_queue__jobs__1
    ON ufw__job_queue__jobs (type, state, scheduled_for ASC);

-- Primarily for job state reporting
CREATE INDEX IX_ufw__job_queue__jobs__2
    ON ufw__job_queue__jobs (state, scheduled_for ASC);

-- Primarily for job expiration
CREATE INDEX IX_ufw__job_queue__jobs__3
    ON ufw__job_queue__jobs (expire_at ASC)
    WHERE expire_at IS NOT NULL;

CREATE UNIQUE INDEX UX_ufw__job_queue__jobs__id ON ufw__job_queue__jobs (id);

CREATE TABLE ufw__job_queue__failures
(
    id               TEXT        NOT NULL PRIMARY KEY,
    job_uid          BIGINT      NOT NULL,
    timestamp        TIMESTAMPTZ NOT NULL,
    error_type       TEXT        NOT NULL,
    error_message    TEXT        NOT NULL,
    error_stacktrace TEXT        NOT NULL,

    CONSTRAINT fk_job FOREIGN KEY (job_uid) REFERENCES ufw__job_queue__jobs (uid)
);

-- Primarily for getting failure for jobs
CREATE INDEX IX_ufw__job_queue__failures_1
    ON ufw__job_queue__failures (job_uid, timestamp);

