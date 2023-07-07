package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.db.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobOptionsBuilder
import io.tpersson.ufw.jobqueue.JobQueueId
import io.tpersson.ufw.jobqueue.JobQueueModuleConfig
import jakarta.inject.Inject
import kotlinx.coroutines.time.withTimeoutOrNull
import java.time.Duration
import java.time.InstantSource

public class JobQueueImpl @Inject constructor(
    private val config: JobQueueModuleConfig,
    private val instantSource: InstantSource,
    private val jobRepository: JobRepository,
) : JobQueueInternal {

    private val pollWaitTime = config.pollWaitTime
    private val defaultJobTimeout = config.defaultJobTimeout
    private val defaultJobRetention = config.defaultJobRetention

    private val signal = ConsumerSignal()

    override suspend fun <TJob : Job>  enqueue(job: TJob, unitOfWork: UnitOfWork, builder: JobOptionsBuilder.() -> Unit) {
        val jobOptions = JobOptionsBuilder().apply(builder)

        val internalJob = InternalJob(
            job = job,
            scheduledFor = jobOptions.scheduleFor ?: instantSource.instant(),
            timeout = jobOptions.timeoutAfter ?: defaultJobTimeout,
            retentionOnFailure = jobOptions.retainOnFailure ?: defaultJobRetention,
            retentionOnSuccess = jobOptions.retainOnSuccess ?: defaultJobRetention,
        )

        jobRepository.insert(internalJob, unitOfWork)

        unitOfWork.addPostCommitHook {
            signal.signal()
        }
    }

    override suspend fun <TJob : Job> pollOne(queueId: JobQueueId<TJob>, timeout: Duration): InternalJob<TJob>? {
        return withTimeoutOrNull(timeout) {
            var next = jobRepository.getNext(queueId)
            while (next == null) {
                signal.wait(pollWaitTime)
                next = jobRepository.getNext(queueId)
            }

            next
        }
    }
}