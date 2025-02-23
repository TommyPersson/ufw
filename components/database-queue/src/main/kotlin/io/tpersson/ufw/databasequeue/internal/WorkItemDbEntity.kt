package io.tpersson.ufw.databasequeue.internal

import java.time.Instant

public data class WorkItemDbEntity(
    val uid: Long,
    val itemId: String,
    val queueId: String,
    val type: String,
    val state: Int,
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

