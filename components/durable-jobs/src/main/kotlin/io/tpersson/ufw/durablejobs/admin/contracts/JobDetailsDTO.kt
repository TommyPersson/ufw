package io.tpersson.ufw.durablejobs.admin.contracts

import java.time.Instant

public data class JobDetailsDTO(
    val queueId: String,
    val jobId: String,
    val jobType: String,
    val jobTypeClass: String,
    val jobTypeDescription: String?,
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