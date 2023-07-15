package io.tpersson.ufw.mediator.middleware.cacheable

import java.time.Duration

public data class CacheConfig(
    val maximumsSize: Long,
    val initialCapacity: Int?,
    val expireAfterWrite: Duration?,
    val expireAfterAccess: Duration?,
)

public class CacheConfigBuilder {
    public var maximumSize: Long = 0
    public var initialCapacity: Int? = null
    public var expireAfterWrite: Duration? = null
    public var expireAfterAccess: Duration? = null

    public fun build(): CacheConfig {
        return CacheConfig(maximumSize, initialCapacity, expireAfterWrite, expireAfterAccess)
    }
}

public fun CacheConfig(block: CacheConfigBuilder.() -> Unit): CacheConfig {
    return CacheConfigBuilder().also(block).build()
}
