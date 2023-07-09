package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobId
import io.tpersson.ufw.jobqueue.JobQueueId
import java.time.Instant

public interface JobRepository {
    public suspend fun insert(job: InternalJob<*>, unitOfWork: UnitOfWork)

    public fun <TJob : Job> getNext(jobQueueId: JobQueueId<TJob>): InternalJob<TJob>?

    public fun <TJob : Job> getById(jobQueueId: JobQueueId<TJob>, jobId: JobId): InternalJob<TJob>?

    public fun <TJob : Job> markAsInProgress(job: InternalJob<TJob>, now: Instant, unitOfWork: UnitOfWork)

    public fun <TJob : Job> maskAsSuccessful(job: InternalJob<TJob>, now: Instant, unitOfWork: UnitOfWork)
}