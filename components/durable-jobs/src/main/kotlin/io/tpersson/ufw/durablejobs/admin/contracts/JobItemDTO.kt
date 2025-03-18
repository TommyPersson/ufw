package io.tpersson.ufw.durablejobs.admin.contracts

import java.time.Instant

public data class JobItemDTO(
    val jobId: String,
    val jobType: String,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
    val stateChangedAt: Instant,
    val numFailures: Int,
)