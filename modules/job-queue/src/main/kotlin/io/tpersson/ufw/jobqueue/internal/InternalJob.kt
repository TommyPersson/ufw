package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobId
import java.time.Duration
import java.time.Instant

public data class InternalJob<TJob : Job>(
    val job: TJob,
    val scheduledFor: Instant,
    val timeout: Duration,
    val retentionOnFailure: Duration,
    val retentionOnSuccess: Duration,
)