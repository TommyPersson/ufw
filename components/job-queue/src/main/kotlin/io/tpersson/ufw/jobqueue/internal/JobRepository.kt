package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobId
import io.tpersson.ufw.jobqueue.JobQueueId
import java.time.Instant
import java.time.InstantSource

public interface JobRepository {
    public suspend fun insert(
        job: InternalJob<*>,
        unitOfWork: UnitOfWork
    )

    public suspend fun <TJob : Job> getNext(
        jobQueueId: JobQueueId<TJob>,
        now: Instant
    ): InternalJob<TJob>?

    public suspend fun <TJob : Job> getById(
        jobQueueId: JobQueueId<TJob>,
        jobId: JobId
    ): InternalJob<TJob>?

    public suspend fun <TJob : Job> markAsInProgress(
        job: InternalJob<TJob>,
        now: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun <TJob : Job> markAsSuccessful(
        job: InternalJob<TJob>,
        now: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun <TJob : Job> markAsFailed(
        job: InternalJob<TJob>, now: Instant,
        watchdogId: String, unitOfWork: UnitOfWork
    )

    public suspend fun <TJob : Job> markAsScheduled(
        job: InternalJob<TJob>,
        now: Instant,
        scheduleFor: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun markStaleJobsAsScheduled(
        now: Instant,
        staleIfWatchdogOlderThan: Instant
    ): Int

    public suspend fun <TJob : Job> updateWatchdog(
        job: InternalJob<TJob>,
        now: Instant,
        watchdogId: String,
    ): Boolean

    public suspend fun debugGetAllJobs(): List<InternalJob<*>>

    public suspend fun debugTruncate(unitOfWork: UnitOfWork)
}