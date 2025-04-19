package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler

public interface DurableMessageHandlerRegistry {
    public fun get(): Set<DurableMessageHandler>

    public fun add(handler: DurableMessageHandler)

    public val topics: Set<String>
}