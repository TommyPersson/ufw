package io.tpersson.ufw.keyvaluestore.storageengine

import java.time.Instant

public data class EntryDataFromRead(
    val value: EntryValue,
    val expiresAt: Instant?,
    val updatedAt: Instant,
    val version: Int,
)

