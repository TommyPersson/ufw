package io.tpersson.ufw.durablecaches.internal

import io.tpersson.ufw.durablecaches.DurableCache
import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.InstantSource
import java.util.concurrent.ConcurrentHashMap

@Singleton
public class DurableCachesImpl @Inject constructor(
    private val keyValueStore: KeyValueStore,
    private val clock: InstantSource,
) : DurableCachesInternal {

    private val _knownCaches = ConcurrentHashMap<String, DurableCacheDefinition<*>>()

    override val knownCaches: Map<String, DurableCacheDefinition<*>>
        get() = _knownCaches

    override fun <TValue : Any> get(definition: DurableCacheDefinition<TValue>): DurableCache<TValue> {
        if (!_knownCaches.containsKey(definition.id)) {
            _knownCaches[definition.id] = definition
            // TODO some kind of registration behavior? 'get' might not get called directly on app start
        }

        return DurableCacheImpl(definition, keyValueStore, clock)
    }
}