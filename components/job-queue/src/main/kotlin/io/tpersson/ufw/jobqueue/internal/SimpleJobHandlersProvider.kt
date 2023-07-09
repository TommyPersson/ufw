package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.JobHandler

public class SimpleJobHandlersProvider(
    private val handlers: Set<JobHandler<*>>
) : JobHandlersProvider {
    override fun get(): Set<JobHandler<*>> {
        return handlers
    }
}