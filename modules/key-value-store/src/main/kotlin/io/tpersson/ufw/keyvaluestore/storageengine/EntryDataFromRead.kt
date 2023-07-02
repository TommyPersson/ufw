package io.tpersson.ufw.keyvaluestore.storageengine

import java.time.Instant

public data class EntryDataFromRead(
    val json: String,
    val expiresAt: Instant?,
    val version: Int,
)

