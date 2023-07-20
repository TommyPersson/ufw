package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler

public class SimpleEventHandlersProvider(
    handlers: Set<TransactionalEventHandler>
) : EventHandlersProvider {

    private val _handlers = handlers.toMutableSet()

    override fun get(): Set<TransactionalEventHandler> {
        return _handlers
    }

    override fun add(handler: TransactionalEventHandler) {
        _handlers.add(handler)
    }
}