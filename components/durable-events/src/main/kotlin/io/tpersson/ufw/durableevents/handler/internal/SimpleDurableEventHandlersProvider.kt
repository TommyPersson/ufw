package io.tpersson.ufw.durableevents.handler.internal

import io.tpersson.ufw.durableevents.handler.DurableEventHandler

public class SimpleDurableEventHandlersProvider(
    private val _handlers: MutableSet<DurableEventHandler>
) : DurableEventHandlersProvider {

    override fun get(): Set<DurableEventHandler> {
        return _handlers
    }

    override fun add(handler: DurableEventHandler) {
        _handlers.add(handler)
    }
}