package io.tpersson.ufw.durableevents.admin.contracts

import java.time.Instant

public data class EventDetailsDTO(
    val queueId: String,
    val eventId: String,
    val eventType: String,
    val eventTypeClass: String,
    val eventTypeDescription: String?,
    val state: String,
    val dataJson: String,
    val metadataJson: String,
    val concurrencyKey: String?,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
    val stateChangedAt: Instant,
    val watchdogTimestamp: Instant?,
    val watchdogOwner: String?,
    val numFailures: Int,
    val expiresAt: Instant?,
)