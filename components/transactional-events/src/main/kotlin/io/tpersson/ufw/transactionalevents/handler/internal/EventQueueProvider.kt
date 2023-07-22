package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.transactionalevents.handler.EventQueueId

public interface EventQueueProvider {
    public val all: List<EventQueue>
    public suspend fun get(queueId: EventQueueId): EventQueue
}