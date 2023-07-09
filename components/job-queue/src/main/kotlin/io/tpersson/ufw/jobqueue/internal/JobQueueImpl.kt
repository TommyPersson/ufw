package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.time.withTimeoutOrNull
import java.lang.Exception
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.util.concurrent.ConcurrentHashMap

@Singleton
public class JobQueueImpl @Inject constructor(
    private val config: JobQueueModuleConfig,
    private val clock: InstantSource,
    private val jobRepository: JobRepository,
) : JobQueueInternal {

    private val logger = createLogger()

    private val pollWaitTime = config.pollWaitTime
    private val defaultJobTimeout = config.defaultJobTimeout
    private val defaultJobRetention = config.defaultJobRetention

    private val signals = ConcurrentHashMap<JobQueueId<*>, ConsumerSignal>()

    override suspend fun <TJob : Job>  enqueue(job: TJob, unitOfWork: UnitOfWork, builder: JobOptionsBuilder.() -> Unit) {
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

        jobRepository.insert(internalJob, unitOfWork)

        unitOfWork.addPostCommitHook {
            getSignal(queueId).signal()
        }
    }

    override suspend fun <TJob : Job> pollOne(queueId: JobQueueId<TJob>, timeout: Duration): InternalJob<TJob>? {
        return withTimeoutOrNull(timeout) {
            var next = jobRepository.getNext(queueId)
            while (next == null) {
                getSignal(queueId).wait(pollWaitTime)
                next = jobRepository.getNext(queueId)
            }

            next
        }
    }

    override fun <TJob : Job> markAsInProgress(job: InternalJob<TJob>, unitOfWork: UnitOfWork) {
        jobRepository.markAsInProgress(job, clock.instant(), unitOfWork)
    }

    override fun <TJob : Job> markAsSuccessful(job: InternalJob<TJob>, unitOfWork: UnitOfWork) {
        jobRepository.maskAsSuccessful(job, clock.instant(), unitOfWork)
    }

    override fun <TJob : Job> rescheduleAt(job: InternalJob<TJob>, at: Instant, unitOfWork: UnitOfWork) {
        TODO("Not yet implemented")
    }

    override fun <TJob : Job> markAsFailed(job: InternalJob<TJob>, error: Exception, unitOfWork: UnitOfWork) {
        TODO("Not yet implemented")
    }

    override fun <TJob : Job> recordFailure(job: InternalJob<TJob>, error: Exception, uow: UnitOfWork) {
        TODO("Not yet implemented")
    }

    override fun <TJob : Job> getNumberOfFailuresFor(job: InternalJob<TJob>): Int {
        TODO("Not yet implemented")
    }

    private fun getSignal(queueId: JobQueueId<*>): ConsumerSignal {
        return signals.getOrPut(queueId) { ConsumerSignal() }
    }
}