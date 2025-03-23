package io.tpersson.ufw.durablecaches

public interface DurableCaches {
    public fun <TValue : Any> get(definition: DurableCacheDefinition<TValue>): DurableCache<TValue>
}