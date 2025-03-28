package io.tpersson.ufw.durablejobs.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.durablejobs.DurableJobQueueId

public data class QueueDetailsDTO(
    val queueId: DurableJobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val status: JobQueueStatusDTO,
    val jobTypes: List<JobType>,
    val applicationModule: ApplicationModuleDTO,
) {
    public data class JobType(
        val type: String,
        val jobClassName: String,
        val description: String?,
    )
}