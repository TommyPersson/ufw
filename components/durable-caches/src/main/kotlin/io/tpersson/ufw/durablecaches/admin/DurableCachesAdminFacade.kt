package io.tpersson.ufw.durablecaches.admin

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.durablecaches.CacheEntry
import io.tpersson.ufw.durablecaches.DurableCacheDefinition

public interface DurableCachesAdminFacade {
    public suspend fun listCaches(paginationOptions: PaginationOptions): PaginatedList<DurableCacheDefinition<*>>

    public suspend fun getByCacheId(cacheId: String): DurableCacheDefinition<*>?

    public suspend fun getNumEntries(cacheId: String): Long

    // TODO dont read value unnecessarily
    public suspend fun listEntries(
        cacheId: String,
        keyPrefix: String,
        paginationOptions: PaginationOptions
    ): PaginatedList<CacheEntry<*>>

    public suspend fun getEntry(cacheId: String, cacheKey: String): CacheEntry<*>?

    public suspend fun invalidateEntry(cacheId: String, cacheKey: String)

    public suspend fun invalidateAllEntries(cacheId: String)
}