package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.WorkItemQueueId

public interface QueueStateChecker {
    public suspend fun isQueuePaused(queueId: WorkItemQueueId): Boolean
}