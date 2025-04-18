package io.tpersson.ufw.durablemessages.admin.contracts

import java.time.Instant

public data class MessageItemDTO(
    val messageId: String,
    val messageType: String,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
    val stateChangedAt: Instant,
    val numFailures: Int,
)