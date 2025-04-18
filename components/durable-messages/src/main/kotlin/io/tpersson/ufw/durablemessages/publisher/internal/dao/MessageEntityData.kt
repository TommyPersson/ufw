package io.tpersson.ufw.durablemessages.publisher.internal.dao

import java.time.Instant

public data class MessageEntityData(
    val uid: Long = 0,
    val id: String,
    val topic: String,
    val type: String,
    val dataJson: String,
    val ceDataJson: String,
    val timestamp: Instant,
)