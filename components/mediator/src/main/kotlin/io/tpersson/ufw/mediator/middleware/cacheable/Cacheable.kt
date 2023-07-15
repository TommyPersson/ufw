package io.tpersson.ufw.mediator.middleware.cacheable

import java.time.Duration

/**
 * Marker interface for the [CacheableMiddleware].
 */
public interface Cacheable<TCacheKey> {
    public val cacheKey: TCacheKey
    public val cacheConfig: CacheConfig
}


