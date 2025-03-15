package io.tpersson.ufw.mediator.middleware.cacheable

/**
 * Marker interface for the [CacheableMiddleware].
 */
public interface Cacheable<TCacheKey : Any> {
    public val cacheKey: TCacheKey
    public val cacheConfig: CacheConfig
}


