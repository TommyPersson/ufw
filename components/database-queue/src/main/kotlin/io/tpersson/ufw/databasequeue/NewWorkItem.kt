package io.tpersson.ufw.databasequeue

import java.time.Instant

public data class NewWorkItem(
    val itemId: WorkItemId,
    val queueId: WorkItemQueueId,
    val type: String,
    val dataJson: String,
    val metadataJson: String,
    val concurrencyKey: String? = null,
    val scheduleFor: Instant,
)

