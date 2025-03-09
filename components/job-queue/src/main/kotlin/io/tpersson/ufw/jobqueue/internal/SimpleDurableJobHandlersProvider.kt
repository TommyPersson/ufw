package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.DurableJobHandler

public class SimpleDurableJobHandlersProvider(
    private val handlers: Set<DurableJobHandler<*>>
) : DurableJobHandlersProvider {
    override fun get(): Set<DurableJobHandler<*>> {
        return handlers
    }
}