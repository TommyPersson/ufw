package io.tpersson.ufw.durableevents.publisher

import io.tpersson.ufw.durableevents.common.DurableEventId
import java.time.Instant

public data class OutgoingEvent(
    val id: DurableEventId,
    val type: String,
    val topic: String,
    val dataJson: String,
    val timestamp: Instant,
)