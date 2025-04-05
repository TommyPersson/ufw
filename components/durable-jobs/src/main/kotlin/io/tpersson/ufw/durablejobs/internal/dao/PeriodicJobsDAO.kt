package io.tpersson.ufw.durablejobs.internal.dao

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablejobs.DurableJobQueueId

public interface PeriodicJobsDAO {
    public suspend fun get(
        queueId: DurableJobQueueId,
        jobType: String,
    ): PeriodicJobStateData?

    public suspend fun put(
        queueId: DurableJobQueueId,
        jobType: String,
        state: PeriodicJobStateData,
        unitOfWork: UnitOfWork,
    )
}

