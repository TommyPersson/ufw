package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler

public interface DurableMessageHandlersRegistry {
    public fun get(): Set<DurableMessageHandler>

    public fun add(handler: DurableMessageHandler)
}