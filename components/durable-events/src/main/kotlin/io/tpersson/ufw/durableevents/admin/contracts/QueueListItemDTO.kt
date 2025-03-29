package io.tpersson.ufw.durableevents.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.durableevents.common.DurableEventQueueId

public data class QueueListItemDTO(
    val queueId: DurableEventQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val status: EventQueueStatusDTO,
    val applicationModule: ApplicationModuleDTO,
)