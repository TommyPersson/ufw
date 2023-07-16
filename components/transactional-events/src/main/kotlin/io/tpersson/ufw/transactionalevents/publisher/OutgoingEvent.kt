package io.tpersson.ufw.transactionalevents.publisher

import io.tpersson.ufw.transactionalevents.EventId
import java.time.Instant

public data class OutgoingEvent(
    val id: EventId,
    val type: String,
    val topic: String,
    val dataJson: String,
    val timestamp: Instant,
)