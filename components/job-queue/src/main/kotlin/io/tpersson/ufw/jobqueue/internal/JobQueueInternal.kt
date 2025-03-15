package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueId

public interface JobQueueInternal : JobQueue {
    public suspend fun getQueueStatistics(queueId: JobQueueId): JobQueueStatistics

    public suspend fun getJobs(
        queueId: JobQueueId,
        state: WorkItemState,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemDbEntity> // TODO "job"-wrappers?

    public suspend fun rescheduleAllFailedJobs(queueId: JobQueueId)
}

public data class JobQueueStatistics(
    val queueId: JobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
)