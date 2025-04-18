package io.tpersson.ufw.durablemessages.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId

public data class QueueDetailsDTO(
    val queueId: DurableMessageQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val status: MessageQueueStatusDTO,
    val messageTypes: List<MessageType>,
    val applicationModule: ApplicationModuleDTO,
) {
    public data class MessageType(
        val typeName: String,
        val className: String,
        val description: String?,
    )
}