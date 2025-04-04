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
        val typeName: String,
        val className: String,
        val description: String?,
        val periodic: Boolean,
        val periodicCron: String?,
        val periodicCronExplanation: String?,
    )
}