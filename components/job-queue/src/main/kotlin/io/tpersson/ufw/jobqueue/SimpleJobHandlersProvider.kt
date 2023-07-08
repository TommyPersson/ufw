package io.tpersson.ufw.jobqueue

public class SimpleJobHandlersProvider(
    private val handlers: Set<JobHandler<*>>
) : JobHandlersProvider {
    override fun get(): Set<JobHandler<*>> {
        return handlers
    }
}