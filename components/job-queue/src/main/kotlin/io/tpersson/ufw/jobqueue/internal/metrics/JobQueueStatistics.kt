package io.tpersson.ufw.jobqueue.internal.metrics

import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobQueueId

public data class JobQueueStatistics<TJob : Job>(
    val queueId: JobQueueId<TJob>,
    val numScheduled: Int,
    val numInProgress: Int,
    val numSuccessful: Int,
    val numFailed: Int,
)