package io.tpersson.ufw.transactionalevents.publisher.internal.dao

import java.time.Instant

public data class EventEntityData(
    val uid: Long = 0,
    val id: String,
    val topic: String,
    val type: String,
    val dataJson: String,
    val ceDataJson: String,
    val timestamp: Instant,
)