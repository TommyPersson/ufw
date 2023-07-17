package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler

public class SimpleEventHandlersProvider(
    private val handlers: Set<TransactionalEventHandler>
) : EventHandlersProvider {

    override fun get(): Set<TransactionalEventHandler> {
        return handlers
    }
}