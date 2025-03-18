package io.tpersson.ufw.durablejobs.internal

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import java.time.Instant

public interface DurableJobQueueInternal : DurableJobQueue {
    public suspend fun getQueueStatistics(queueId: DurableJobQueueId): JobQueueStatistics

    public suspend fun getJobs(
        queueId: DurableJobQueueId,
        state: WorkItemState,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemDbEntity> // TODO "job"-wrappers?

    public suspend fun getJob(
        queueId: DurableJobQueueId,
        jobId: DurableJobId,
    ): WorkItemDbEntity? // TODO "job"-wrappers?

    public suspend fun rescheduleAllFailedJobs(queueId: DurableJobQueueId)

    public suspend fun deleteAllFailedJobs(queueId: DurableJobQueueId)

    public suspend fun getJobFailures(
        queueId: DurableJobQueueId,
        jobId: DurableJobId,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemFailureDbEntity>  // TODO "job"-wrappers?

    public suspend fun deleteFailedJob(
        queueId: DurableJobQueueId,
        jobId: DurableJobId
    ): Boolean

    public suspend fun rescheduleFailedJob(
        queueId: DurableJobQueueId,
        jobId: DurableJobId,
        rescheduleAt: Instant
    )

    public suspend fun cancelJob(
        queueId: DurableJobQueueId,
        jobId: DurableJobId,
        now: Instant
    )
}

public data class JobQueueStatistics(
    val queueId: DurableJobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
)