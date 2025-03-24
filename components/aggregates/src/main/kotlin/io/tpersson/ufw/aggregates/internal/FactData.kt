package io.tpersson.ufw.aggregates.internal

import java.time.Instant
import java.util.*

public data class FactData(
    val id: UUID,
    val aggregateId: String,
    val type: String,
    val json: String,
    val timestamp: Instant,
    val version: Long,
)