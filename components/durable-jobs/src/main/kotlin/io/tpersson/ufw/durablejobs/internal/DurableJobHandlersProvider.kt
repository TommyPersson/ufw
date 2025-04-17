package io.tpersson.ufw.durablejobs.internal

import io.tpersson.ufw.durablejobs.DurableJobHandler

public interface DurableJobHandlersProvider {
    public fun get(): Set<DurableJobHandler<*>>

    public fun add(handler: DurableJobHandler<*>)
}