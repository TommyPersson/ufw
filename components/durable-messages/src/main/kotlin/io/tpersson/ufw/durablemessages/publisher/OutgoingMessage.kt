package io.tpersson.ufw.durablemessages.publisher

import io.tpersson.ufw.durablemessages.common.DurableMessageId
import java.time.Instant

public data class OutgoingMessage(
    val id: DurableMessageId,
    val key: String?,
    val type: String,
    val topic: String,
    val dataJson: String, // TODO metadata
    val timestamp: Instant,
)