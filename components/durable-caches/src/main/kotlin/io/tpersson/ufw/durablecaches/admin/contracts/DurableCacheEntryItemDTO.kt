package io.tpersson.ufw.durablecaches.admin.contracts

import java.time.Instant

public data class DurableCacheEntryItemDTO(
    val key: String,
    val expiresAt: Instant?,
    val cachedAt: Instant,
)