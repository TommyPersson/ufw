package io.tpersson.ufw.durablecaches

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions

public interface DurableCache<TValue : Any> {
    public val definition: DurableCacheDefinition<TValue>

    /**
     * @note Will bypass the in-memory cache, if any.
     */
    public suspend fun list(keyPrefix: String, paginationOptions: PaginationOptions): PaginatedList<CacheEntry<TValue>>

    public suspend fun get(key: String): TValue?

    public suspend fun getOrPut(
        key: String,
        factory: suspend (key: String) -> TValue
    ): TValue

    public suspend fun put(key: String, value: TValue)

    public suspend fun remove(key: String)

    public suspend fun removeAll()

    public suspend fun getNumEntries(): Long
}
