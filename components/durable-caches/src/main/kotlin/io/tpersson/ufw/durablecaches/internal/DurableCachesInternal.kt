package io.tpersson.ufw.durablecaches.internal

import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import io.tpersson.ufw.durablecaches.DurableCaches

public interface DurableCachesInternal : DurableCaches {
    public val knownCaches: Map<String, DurableCacheDefinition<*>>
}