package io.tpersson.ufw.durableevents.handler

import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.durableevents.common.DurableEventQueueId

public interface DurableEventHandler {
    public val queueId: DurableEventQueueId get() = DurableEventQueueId(this::class.simpleName!!)

    public suspend fun onFailure(event: Any, error: Exception, context: DurableEventFailureContext): FailureAction =
        FailureAction.GiveUp
}

