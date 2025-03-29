package io.tpersson.ufw.durableevents.admin.contracts

import java.time.Instant

public data class EventItemDTO(
    val eventId: String,
    val eventType: String,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
    val stateChangedAt: Instant,
    val numFailures: Int,
)