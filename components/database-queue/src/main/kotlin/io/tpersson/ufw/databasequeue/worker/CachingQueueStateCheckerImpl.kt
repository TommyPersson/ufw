package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkQueueState
import io.tpersson.ufw.databasequeue.internal.WorkQueuesDAO
import jakarta.inject.Inject

public class CachingQueueStateCheckerImpl @Inject constructor(
    private val workQueuesDAO: WorkQueuesDAO,
) : QueueStateChecker {
    override suspend fun isQueuePaused(queueId: WorkItemQueueId): Boolean {
        // TODO actual caching
        return workQueuesDAO.getWorkQueue(queueId)?.state == WorkQueueState.PAUSED.name
    }

}