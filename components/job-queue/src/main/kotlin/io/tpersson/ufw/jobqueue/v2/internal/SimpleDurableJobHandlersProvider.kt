package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.jobqueue.v2.DurableJobHandler

public class SimpleDurableJobHandlersProvider(
    private val handlers: Set<DurableJobHandler<*>>
) : DurableJobHandlersProvider {
    override fun get(): Set<DurableJobHandler<*>> {
        return handlers
    }
}