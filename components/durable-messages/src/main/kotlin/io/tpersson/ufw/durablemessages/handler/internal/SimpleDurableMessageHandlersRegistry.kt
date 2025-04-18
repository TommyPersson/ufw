package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler

public class SimpleDurableMessageHandlersRegistry(
    private val _handlers: MutableSet<DurableMessageHandler>
) : DurableMessageHandlersRegistry {

    override fun get(): Set<DurableMessageHandler> {
        return _handlers
    }

    override fun add(handler: DurableMessageHandler) {
        _handlers.add(handler)
    }
}