package io.tpersson.ufw.durablemessages.admin.contracts

import java.time.Instant

public data class MessageDetailsDTO(
    val queueId: String,
    val messageId: String,
    val messageType: String,
    val messageTypeClass: String,
    val messageTypeDescription: String?,
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