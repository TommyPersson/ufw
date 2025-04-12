package io.tpersson.ufw.durablejobs.periodic.internal.dao

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import java.time.Instant

public interface PeriodicJobsDAO {
    public suspend fun get(
        queueId: DurableJobQueueId,
        jobType: String,
    ): PeriodicJobStateData?

    public suspend fun getAll(
        paginationOptions: PaginationOptions
    ): PaginatedList<PeriodicJobStateData>

    public suspend fun setSchedulingInfo(
        queueId: DurableJobQueueId,
        jobType: String,
        nextSchedulingAttempt: Instant?,
        lastSchedulingAttempt: Instant?,
        unitOfWork: UnitOfWork,
    )

    public suspend fun setExecutionInfo(
        queueId: DurableJobQueueId,
        jobType: String,
        state: WorkItemState?,
        stateChangeTimestamp: Instant?,
        unitOfWork: UnitOfWork,
    )

    public suspend fun debugTruncate()
}

