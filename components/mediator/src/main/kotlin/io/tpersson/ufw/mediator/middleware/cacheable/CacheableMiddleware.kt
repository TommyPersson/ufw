package io.tpersson.ufw.mediator.middleware.cacheable

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.middleware.StandardMiddlewarePriorities
import jakarta.inject.Inject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

public class CacheableMiddleware @Inject constructor(
) : Middleware<Cacheable<*>, Any?> {

    override val priority: Int = StandardMiddlewarePriorities.Cacheable

    private val caches = ConcurrentHashMap<KClass<out Cacheable<*>>, Cache<Any, Optional<Any>>>()

    override suspend fun handle(
        request: Cacheable<*>,
        context: Context,
        next: suspend (request: Cacheable<*>, context: Context) -> Any?
    ): Any? {
        val cache = caches.getOrPut(request::class) { createCache(request) }

        val existing = cache.getIfPresent(request.cacheKey)
        if (existing != null) {
            return existing.orElse(null)
        }

        val result = next(request, context)

        cache.put(request.cacheKey, Optional.ofNullable(result))

        return result
    }

    private fun createCache(request: Cacheable<*>): Cache<Any, Optional<Any>>? =
        Caffeine.newBuilder()
            .maximumSize(request.cacheConfig.maximumsSize)
            .let {
                if (request.cacheConfig.expireAfterAccess != null) {
                    it.expireAfterAccess(request.cacheConfig.expireAfterAccess)
                } else it
            }
            .let {
                if (request.cacheConfig.expireAfterWrite != null) {
                    it.expireAfterWrite(request.cacheConfig.expireAfterWrite)
                } else it
            }
            .let {
                if (request.cacheConfig.initialCapacity != null) {
                    it.initialCapacity(request.cacheConfig.initialCapacity!!)
                } else it
            }
            .build()
}