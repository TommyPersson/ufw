package io.tpersson.ufw.durablemessages.common

import java.time.Instant

public data class IncomingMessage(
    val id: DurableMessageId,
    val type: String,
    val topic: String,
    val dataJson: String,
    val metadata: Map<String, String?>,
    val timestamp: Instant,
)