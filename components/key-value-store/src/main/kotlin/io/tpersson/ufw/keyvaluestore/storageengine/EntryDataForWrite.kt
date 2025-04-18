package io.tpersson.ufw.keyvaluestore.storageengine

import java.time.Instant

public data class EntryDataForWrite(
    val value: EntryValue,
    val expiresAt: Instant?,
    val now: Instant,
)