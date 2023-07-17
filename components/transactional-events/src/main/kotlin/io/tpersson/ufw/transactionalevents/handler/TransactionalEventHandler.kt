package io.tpersson.ufw.transactionalevents.handler

public abstract class TransactionalEventHandler {
    public val eventQueueId: EventQueueId get() = EventQueueId(this::class.simpleName!!)
}



