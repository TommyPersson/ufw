package io.tpersson.ufw.durablecaches.admin

import io.ktor.http.*
import io.tpersson.ufw.admin.raise
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.durablecaches.CacheEntry
import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import io.tpersson.ufw.durablecaches.internal.DurableCachesInternal
import jakarta.inject.Inject

public class DurableCachesAdminFacadeImpl @Inject constructor(
    private val durableCaches: DurableCachesInternal,
): DurableCachesAdminFacade {
    override suspend fun listCaches(paginationOptions: PaginationOptions): PaginatedList<DurableCacheDefinition<*>> {
        val items = durableCaches.knownCaches.values
            .drop(paginationOptions.offset)
            .take(paginationOptions.limit + 1)

        return PaginatedList(
            items = items.take(paginationOptions.limit),
            options = paginationOptions,
            hasMoreItems = items.size > paginationOptions.limit
        )
    }

    override suspend fun getByCacheId(cacheId: String): DurableCacheDefinition<*>? {
        return durableCaches.knownCaches[cacheId]
    }

    override suspend fun getNumEntries(cacheId: String): Long {
        val definition = durableCaches.knownCaches[cacheId]
            ?: return 0

        return durableCaches.get(definition).getNumEntries()
    }

    override suspend fun listEntries(
        cacheId: String,
        keyPrefix: String,
        paginationOptions: PaginationOptions
    ): PaginatedList<CacheEntry<*>> {
        val definition = durableCaches.knownCaches[cacheId]
            ?: return PaginatedList.empty(paginationOptions)

        val cache = durableCaches.get(definition)

        // TODO dont read value unnecessarily
        return cache.list(keyPrefix, paginationOptions)
    }

    override suspend fun getEntry(cacheId: String, cacheKey: String): CacheEntry<*>? {
        val definition = durableCaches.knownCaches[cacheId]
            ?: return null

        if (definition.containsSensitiveData) {
            HttpStatusCode.Forbidden.raise()
        }

        val cache = durableCaches.get(definition)

        return cache.getEntry(cacheKey)
    }

    override suspend fun invalidateEntry(cacheId: String, cacheKey: String) {
        val definition = durableCaches.knownCaches[cacheId]
            ?: return

        durableCaches.get(definition).remove(cacheKey)
    }

    override suspend fun invalidateAllEntries(cacheId: String) {
        val definition = durableCaches.knownCaches[cacheId]
            ?: return

        durableCaches.get(definition).removeAll()
    }
}