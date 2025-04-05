package io.tpersson.ufw.durablejobs.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.durablejobs.DurableJobQueueId

public data class QueueListItemDTO(
    val queueId: DurableJobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val status: JobQueueStatusDTO,
    val hasOnlyPeriodicJobTypes: Boolean,
    val applicationModule: ApplicationModuleDTO,
)