package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler

public class SimpleDurableMessageHandlersRegistry(
    private val _handlers: MutableSet<DurableMessageHandler>
) : DurableMessageHandlerRegistry {

    override fun get(): Set<DurableMessageHandler> {
        return _handlers
    }

    override fun add(handler: DurableMessageHandler) {
        _handlers.add(handler)
    }

    public override val topics: Set<String> by Memoized({ _handlers }) {
        it.flatMap { handler -> handler.findHandlerMethods().map { method -> method.messageTopic } }.toSet()
    }
}