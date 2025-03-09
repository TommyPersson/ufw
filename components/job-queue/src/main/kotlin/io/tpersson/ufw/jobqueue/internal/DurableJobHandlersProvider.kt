package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.DurableJobHandler

public interface DurableJobHandlersProvider {
    public fun get(): Set<DurableJobHandler<*>>
}