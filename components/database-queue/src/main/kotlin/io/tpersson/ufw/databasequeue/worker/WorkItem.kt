package io.tpersson.ufw.databasequeue.worker

import java.time.Instant

public data class WorkItem(
    val uid: Long,
    val itemId: String,
    val queueId: String,
    val type: String,
    val state: Int,
    val dataJson: String,
    val metadataJson: String,
    val concurrencyKey: String?,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
)
