package io.tpersson.ufw.durableevents.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.durableevents.common.DurableEventQueueId

public data class QueueDetailsDTO(
    val queueId: DurableEventQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val status: EventQueueStatusDTO,
    val eventTypes: List<EventType>,
    val applicationModule: ApplicationModuleDTO,
) {
    public data class EventType(
        val typeName: String,
        val className: String,
        val description: String?,
    )
}