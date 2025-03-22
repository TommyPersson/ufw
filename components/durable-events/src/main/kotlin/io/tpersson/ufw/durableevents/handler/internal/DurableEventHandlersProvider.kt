package io.tpersson.ufw.durableevents.handler.internal

import io.tpersson.ufw.durableevents.handler.DurableEventHandler

public interface DurableEventHandlersProvider {
    public fun get(): Set<DurableEventHandler>
}