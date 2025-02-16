package io.tpersson.ufw.databasequeue

import java.time.Instant

public data class NewWorkItem(
    val itemId: String,
    val queueId: String,
    val type: String,
    val dataJson: String,
    val metadataJson: String,
    val concurrencyKey: String? = null,
    val scheduleFor: Instant,
)