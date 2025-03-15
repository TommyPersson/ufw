package io.tpersson.ufw.durablejobs.internal

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.DurableJobQueueId

public interface DurableJobQueueInternal : DurableJobQueue {
    public suspend fun getQueueStatistics(queueId: DurableJobQueueId): JobQueueStatistics

    public suspend fun getJobs(
        queueId: DurableJobQueueId,
        state: WorkItemState,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemDbEntity> // TODO "job"-wrappers?

    public suspend fun rescheduleAllFailedJobs(queueId: DurableJobQueueId)
}

public data class JobQueueStatistics(
    val queueId: DurableJobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
)