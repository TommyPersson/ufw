package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.databasequeue.WorkItemQueueId

public data class WorkItemQueueStatistics(
    val queueId: WorkItemQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numSuccessful: Int,
    val numFailed: Int,
)