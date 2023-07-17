package io.tpersson.ufw.transactionalevents.handler

import io.tpersson.ufw.transactionalevents.EventId
import java.time.Instant

public data class IncomingEvent(
    val id: EventId,
    val type: String,
    val topic: String,
    val dataJson: String,
    val timestamp: Instant,
)