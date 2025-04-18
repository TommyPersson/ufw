package io.tpersson.ufw.durablemessages.handler

import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId

public interface DurableMessageHandler {
    public val queueId: DurableMessageQueueId get() = DurableMessageQueueId(this::class.simpleName!!)

    public suspend fun onFailure(message: Any, error: Exception, context: DurableMessageFailureContext): FailureAction =
        FailureAction.GiveUp
}

