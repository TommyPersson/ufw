package io.tpersson.ufw.durablejobs.admin.contracts

import io.tpersson.ufw.durablejobs.DurableJobQueueId

public data class QueueDetailsDTO(
    val queueId: DurableJobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val jobTypes: List<JobType>,
) {
    public data class JobType(
        val type: String,
        val jobClassName: String,
        val description: String?,
    )
}