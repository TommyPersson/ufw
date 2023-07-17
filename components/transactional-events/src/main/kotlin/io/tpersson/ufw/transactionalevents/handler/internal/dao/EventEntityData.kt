package io.tpersson.ufw.transactionalevents.handler.internal.dao

import java.time.Instant

public data class EventEntityData(
    val uid: Long? = 0,
    val queueId: String,
    val id: String,
    val topic: String,
    val type: String,
    val dataJson: String,
    val ceDataJson: String,
    val timestamp: Instant,
    val state: Int,
    val createdAt: Instant,
    val scheduledFor: Instant,
    val stateChangedAt: Instant,
    val watchdogTimestamp: Instant?,
    val watchdogOwner: String?,
    val expireAt: Instant?
)