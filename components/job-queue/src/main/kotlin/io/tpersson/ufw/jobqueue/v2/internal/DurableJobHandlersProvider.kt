package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.jobqueue.v2.DurableJobHandler

public interface DurableJobHandlersProvider {
    public fun get(): Set<DurableJobHandler<*>>
}