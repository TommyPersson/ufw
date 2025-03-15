package io.tpersson.ufw.databasequeue.internal

import java.time.Instant

public data class WorkQueueDbEntity(
    val queueId: String,
    val state: String,
    val stateChangedAt: Instant,
)