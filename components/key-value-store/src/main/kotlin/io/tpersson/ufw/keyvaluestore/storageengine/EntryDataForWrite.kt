package io.tpersson.ufw.keyvaluestore.storageengine

import java.time.Instant

public data class EntryDataForWrite(
    val json: String,
    val expiresAt: Instant?,
    val updatedAt: Instant
)