package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueId
import java.lang.Exception
import java.time.Duration
import java.time.Instant

public interface JobQueueInternal : JobQueue {
    public suspend fun <TJob : Job> pollOne(queueId: JobQueueId<TJob>, timeout: Duration): InternalJob<TJob>?
    public suspend fun <TJob : Job> markAsInProgress(job: InternalJob<TJob>, unitOfWork: UnitOfWork)
    public suspend fun <TJob : Job> markAsSuccessful(job: InternalJob<TJob>, unitOfWork: UnitOfWork)
    public suspend fun <TJob : Job> rescheduleAt(job: InternalJob<TJob>, at: Instant, unitOfWork: UnitOfWork)
    public suspend fun <TJob : Job> markAsFailed(job: InternalJob<TJob>, error: Exception, unitOfWork: UnitOfWork)
    public suspend fun <TJob : Job> recordFailure(job: InternalJob<TJob>, error: Exception, uow: UnitOfWork)
    public suspend fun <TJob : Job> getNumberOfFailuresFor(job: InternalJob<TJob>): Int
}