package io.tpersson.ufw.durableevents.handler.internal

import io.tpersson.ufw.durableevents.handler.DurableEventHandler

public class SimpleDurableEventHandlersProvider(
    private val handlers: Set<DurableEventHandler>
) : DurableEventHandlersProvider {
    override fun get(): Set<DurableEventHandler> {
        return handlers
    }
}