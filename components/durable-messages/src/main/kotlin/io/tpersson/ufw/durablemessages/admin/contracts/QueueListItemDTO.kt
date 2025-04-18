package io.tpersson.ufw.durablemessages.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId

public data class QueueListItemDTO(
    val queueId: DurableMessageQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val status: MessageQueueStatusDTO,
    val applicationModule: ApplicationModuleDTO,
)