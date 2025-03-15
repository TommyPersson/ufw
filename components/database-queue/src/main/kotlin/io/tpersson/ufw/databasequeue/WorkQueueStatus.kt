package io.tpersson.ufw.databasequeue

import java.time.Instant

public data class WorkQueueStatus(
    val state: WorkQueueState,
    val stateChangedAt: Instant,
)