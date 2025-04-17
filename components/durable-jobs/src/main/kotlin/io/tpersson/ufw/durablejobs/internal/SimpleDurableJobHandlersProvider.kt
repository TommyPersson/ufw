package io.tpersson.ufw.durablejobs.internal

import io.tpersson.ufw.durablejobs.DurableJobHandler

public class SimpleDurableJobHandlersProvider(
    private val _handlers: MutableSet<DurableJobHandler<*>>
) : DurableJobHandlersProvider {
    override fun get(): Set<DurableJobHandler<*>> {
        return _handlers
    }

    override fun add(handler: DurableJobHandler<*>) {
        _handlers.add(handler)
    }
}