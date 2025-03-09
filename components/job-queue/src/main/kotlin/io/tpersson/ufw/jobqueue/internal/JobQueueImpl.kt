package io.tpersson.ufw.jobqueue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.jobqueue.*
import io.tpersson.ufw.jobqueue.v2.DurableJob
import io.tpersson.ufw.jobqueue.v2.JobId
import io.tpersson.ufw.jobqueue.v2.internal.jobDefinition2
import io.tpersson.ufw.jobqueue.v2.internal.toWorkItemId
import io.tpersson.ufw.jobqueue.v2.internal.toWorkItemQueueId
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.time.withTimeoutOrNull
import java.lang.Exception
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
public class JobQueueImpl @Inject constructor(
    private val config: JobQueueConfig,
    private val clock: InstantSource,
    private val jobsDAO: JobsDAO,
    private val jobFailureRepository: JobFailureRepository,
    private val workItemsDAO: WorkItemsDAO,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : JobQueueInternal {

    private val logger = createLogger()

    private val pollWaitTime = config.pollWaitTime

    private val signals = ConcurrentHashMap<JobQueueId<*>, ConsumerSignal>()

    override suspend fun <TJob : Job> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit
    ) {
        val jobOptions = JobOptionsBuilder().apply(builder)

        val queueId = JobQueueId(job::class)

        val internalJob = InternalJob(
            uid = 0,
            job = job,
            state = JobState.Scheduled,
            createdAt = clock.instant(),
            scheduledFor = jobOptions.scheduleFor ?: clock.instant(),
            stateChangedAt = clock.instant(),
            expireAt = null,
        )

        jobsDAO.insert(internalJob, unitOfWork)

        unitOfWork.addPostCommitHook {
            getSignal(queueId).signal()
        }
    }


    override suspend fun <TJob : DurableJob> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit
    ) {
        val jobOptions = JobOptionsBuilder().apply(builder)

        val jobDefinition = job::class.jobDefinition2

        val queueId = jobDefinition.queueId
        val type = jobDefinition.type

        workItemsDAO.scheduleNewItem(
            newItem = NewWorkItem(
                queueId = queueId.toWorkItemQueueId(),
                type = type,
                itemId = job.id.toWorkItemId(),
                dataJson = objectMapper.writeValueAsString(job), // TODO move to new 'WorkItemsQueue'?
                metadataJson = "{}",
                concurrencyKey = null,
                scheduleFor = jobOptions.scheduleFor ?: clock.instant(),
            ),
            now = clock.instant(),
            unitOfWork = unitOfWork,
        )

        unitOfWork.addPostCommitHook {
            //getSignal(queueId).signal()
        }
    }

    // Used by tests
    internal fun <TJob : Job> debugSignal(queueId: JobQueueId<TJob>) {
        getSignal(queueId).signal()
    }

    override suspend fun <TJob : Job> pollOne(
        queueId: JobQueueId<TJob>,
        timeout: Duration,
        watchdogId: String
    ): InternalJob<TJob>? {
        return withTimeoutOrNull(timeout) {
            var next = jobsDAO.takeNext(queueId, clock.instant(), watchdogId)
            while (next == null) {
                getSignal(queueId).wait(pollWaitTime)
                next = jobsDAO.takeNext(queueId, clock.instant(), watchdogId)
            }

            next
        }
    }

    override suspend fun <TJob : Job> markAsSuccessful(
        job: InternalJob<TJob>,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        val now = clock.instant()
        val expireAt = now + config.successfulJobRetention
        jobsDAO.markAsSuccessful(job, now, expireAt, watchdogId, unitOfWork)
    }

    override suspend fun <TJob : Job> rescheduleAt(
        job: InternalJob<TJob>,
        at: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        jobsDAO.markAsScheduled(job, clock.instant(), at, watchdogId, unitOfWork)
    }

    override suspend fun <TJob : Job> markAsFailed(
        job: InternalJob<TJob>,
        error: Exception,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        val now = clock.instant()
        val expireAt = now + config.failedJobRetention
        jobsDAO.markAsFailed(job, now, expireAt, watchdogId, unitOfWork)
    }

    override suspend fun <TJob : Job> recordFailure(job: InternalJob<TJob>, error: Exception, uow: UnitOfWork) {
        val jobFailure = JobFailure(
            id = UUID.randomUUID(),
            jobUid = job.uid!!,
            timestamp = clock.instant(),
            errorType = error::class.simpleName!!,
            errorMessage = error.message ?: "<no message>",
            errorStackTrace = error.stackTraceToString()
        )

        jobFailureRepository.insert(jobFailure, unitOfWork = uow)
    }

    override suspend fun <TJob : Job> getNumberOfFailuresFor(job: InternalJob<TJob>): Int {
        return jobFailureRepository.getNumberOfFailuresFor(job)
    }

    override suspend fun getQueueStatistics(queueId: io.tpersson.ufw.jobqueue.v2.JobQueueId): JobQueueStatistics {
        val workItemQueueStatistics = workItemsDAO.getQueueStatistics(queueId.toWorkItemQueueId())
        return JobQueueStatistics(
            queueId = queueId,
            numScheduled = workItemQueueStatistics.numScheduled,
            numPending = workItemQueueStatistics.numPending,
            numInProgress = workItemQueueStatistics.numInProgress,
            numFailed = workItemQueueStatistics.numFailed,
        )
    }

    private fun getSignal(queueId: JobQueueId<*>): ConsumerSignal {
        return signals.getOrPut(queueId) { ConsumerSignal() }
    }
}
