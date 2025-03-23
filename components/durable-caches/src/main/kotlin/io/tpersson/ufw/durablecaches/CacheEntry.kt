package io.tpersson.ufw.durablecaches

import java.time.Instant

public data class CacheEntry<TValue>(
    val key: String,
    val value: TValue,
    val cachedAt: Instant,
    val expiresAt: Instant?,
)