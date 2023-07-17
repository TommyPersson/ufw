package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler

public interface EventHandlersProvider {
    public fun get(): Set<TransactionalEventHandler>
}