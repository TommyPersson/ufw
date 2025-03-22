package io.tpersson.ufw.durableevents.common

import java.time.Instant

public data class IncomingEvent(
    val id: DurableEventId,
    val type: String,
    val topic: String,
    val dataJson: String,
    val timestamp: Instant,
)