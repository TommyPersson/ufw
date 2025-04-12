package io.tpersson.ufw.databasequeue

import java.time.Instant

public data class WorkItemStateChange(
    val queueId: WorkItemQueueId,
    val itemId: WorkItemId,
    val itemType: String,
    val fromState: WorkItemState?,
    val toState: WorkItemState,
    val timestamp: Instant,
)