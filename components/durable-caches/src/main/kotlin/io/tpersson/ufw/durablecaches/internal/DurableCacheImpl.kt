package io.tpersson.ufw.durablecaches.internal

import com.github.benmanes.caffeine.cache.Caffeine
import io.tpersson.ufw.core.utils.InstantSourceTicker
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.durablecaches.CacheEntry
import io.tpersson.ufw.durablecaches.DurableCache
import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import java.time.InstantSource
import kotlin.reflect.KClass

public class DurableCacheImpl<TValue : Any>(
    override val definition: DurableCacheDefinition<TValue>,
    private val keyValueStore: KeyValueStore,
    private val clock: InstantSource
) : DurableCache<TValue> {

    private val keyPrefix = "__dc__cache:${definition.id}:"

    private val inMemoryCache = definition.inMemoryExpiration?.let { expiration ->
        Caffeine.newBuilder()
            .expireAfterWrite(expiration)
            .ticker(InstantSourceTicker(clock))
            .build<String, TValue>()
    }

    override suspend fun list(keyPrefix: String, paginationOptions: PaginationOptions): PaginatedList<CacheEntry<TValue>> {
        val finalPrefix = this.keyPrefix + keyPrefix

        val entries = keyValueStore.list(
            prefix = finalPrefix,
            limit = paginationOptions.limit + 1,
            offset = paginationOptions.offset
        )

        val items = entries.take(paginationOptions.limit).map {
            @Suppress("UNCHECKED_CAST")
            CacheEntry(
                key = it.key.substringAfter(this.keyPrefix),
                value = it.parseAs(definition.valueType as KClass<TValue>).value,
                cachedAt = it.updatedAt,
                expiresAt = it.expiresAt
            )
        }

        return PaginatedList(
            options = paginationOptions,
            items = items,
            hasMoreItems = entries.size > paginationOptions.limit
        )
    }

    // TODO add unit test
    override suspend fun getEntry(key: String): CacheEntry<TValue>? {
        val kvsKey = getKvsKey(key)

        val kvsEntry = keyValueStore.get(kvsKey)
            ?: return null

        return CacheEntry(
            key = kvsEntry.key.name.substringAfter(this.keyPrefix),
            value = kvsEntry.value,
            cachedAt = kvsEntry.updatedAt,
            expiresAt = kvsEntry.expiresAt
        )
    }

    override suspend fun get(key: String): TValue? {
        val inMemoryValue = inMemoryCache?.getIfPresent(key)
        if (inMemoryValue != null) {
            return inMemoryValue
        }

        val kvsKey = getKvsKey(key)

        val entry = keyValueStore.get(kvsKey)
            ?: return null

        inMemoryCache?.put(key, entry.value)

        return entry.value
    }

    override suspend fun getOrPut(
        key: String,
        factory: suspend (key: String) -> TValue
    ): TValue {
        val existing = get(key)
        if (existing != null) {
            return existing
        }

        put(key, factory(key))

        return get(key)!!
    }

    override suspend fun put(key: String, value: TValue) {
        val kvsKey = getKvsKey(key)

        keyValueStore.put(kvsKey, value, ttl = definition.expiration)
        inMemoryCache?.put(key, value)
    }

    override suspend fun remove(key: String) {
        val kvsKey = getKvsKey(key)

        keyValueStore.remove(kvsKey)
        inMemoryCache?.invalidate(key)
    }

    override suspend fun removeAll() {
        keyValueStore.removeAll(keyPrefix)
        inMemoryCache?.invalidateAll()
    }

    override suspend fun getNumEntries(): Long {
        return keyValueStore.getNumEntries(keyPrefix)
    }

    private fun getKvsKey(key: String) = KeyValueStore.Key("${keyPrefix}${key}", definition.valueType)
}