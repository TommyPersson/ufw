package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobQueueId

public interface JobRepository {
    public suspend fun insert(job: InternalJob<*>, unitOfWork: UnitOfWork)

    public fun <TJob : Job> getNext(jobQueueId: JobQueueId<TJob>): InternalJob<TJob>?
}