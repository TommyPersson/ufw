package io.tpersson.ufw.durablejobs.internal

import io.tpersson.ufw.durablejobs.DurableJobHandler

public class SimpleDurableJobHandlersProvider(
    private val handlers: Set<DurableJobHandler<*>>
) : DurableJobHandlersProvider {
    override fun get(): Set<DurableJobHandler<*>> {
        return handlers
    }
}