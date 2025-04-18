package io.tpersson.ufw.keyvaluestore.storageengine

import java.time.Instant

public data class EntryMetadata(
    val key: String,
    val expiresAt: Instant?,
    val updatedAt: Instant,
    val createdAt: Instant,
    val version: Int,
)

