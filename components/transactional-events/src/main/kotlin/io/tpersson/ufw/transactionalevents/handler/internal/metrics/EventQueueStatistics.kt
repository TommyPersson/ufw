package io.tpersson.ufw.transactionalevents.handler.internal.metrics

import io.tpersson.ufw.transactionalevents.handler.EventQueueId

public data class EventQueueStatistics(
    val queueId: EventQueueId,
    val numScheduled: Int,
    val numInProgress: Int,
    val numSuccessful: Int,
    val numFailed: Int,
)