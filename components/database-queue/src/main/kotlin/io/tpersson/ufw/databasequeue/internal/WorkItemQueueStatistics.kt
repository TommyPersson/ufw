package io.tpersson.ufw.databasequeue.internal

public data class WorkItemQueueStatistics(
    val queueId: String,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numSuccessful: Int,
    val numFailed: Int,
)