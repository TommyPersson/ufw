package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkQueueState
import java.time.Instant

public interface WorkQueuesDAO {
    public suspend fun getWorkQueue(queueId: WorkItemQueueId): WorkQueueDbEntity?

    public suspend fun setWorkQueueState(
        queueId: WorkItemQueueId,
        state: WorkQueueState,
        now: Instant
        // TODO optional UnitOfWork
    )

    public suspend fun debugTruncate()
}