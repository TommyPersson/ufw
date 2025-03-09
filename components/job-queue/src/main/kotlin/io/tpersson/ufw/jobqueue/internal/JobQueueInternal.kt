package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueId
import java.lang.Exception
import java.time.Duration
import java.time.Instant

public interface JobQueueInternal : JobQueue {
    public suspend fun <TJob : Job> pollOne(
        queueId: JobQueueId<TJob>,
        timeout: Duration,
        watchdogId: String,
    ): InternalJob<TJob>?

    public suspend fun <TJob : Job> markAsSuccessful(
        job: InternalJob<TJob>,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun <TJob : Job> rescheduleAt(
        job: InternalJob<TJob>,
        at: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun <TJob : Job> markAsFailed(
        job: InternalJob<TJob>,
        error: Exception,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun <TJob : Job> recordFailure(
        job: InternalJob<TJob>,
        error: Exception,
        uow: UnitOfWork
    )

    public suspend fun <TJob : Job> getNumberOfFailuresFor(job: InternalJob<TJob>): Int

    public suspend fun getQueueStatistics(queueId: io.tpersson.ufw.jobqueue.v2.JobQueueId): JobQueueStatistics
}

public data class JobQueueStatistics(
    val queueId: io.tpersson.ufw.jobqueue.v2.JobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
)