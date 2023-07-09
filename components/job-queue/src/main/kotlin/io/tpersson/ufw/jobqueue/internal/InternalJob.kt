package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobId
import io.tpersson.ufw.jobqueue.JobState
import java.time.Duration
import java.time.Instant

public data class InternalJob<TJob : Job>(
    val uid: Long?,
    val job: TJob,
    val state: JobState,
    val createdAt: Instant,
    val scheduledFor: Instant,
    val stateChangedAt: Instant,
    val expireAt: Instant?,
)