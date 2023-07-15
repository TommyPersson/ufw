package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.core.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.jobqueue.internal.*
import io.tpersson.ufw.jobqueue.internal.exceptions.JobOwnershipLostException
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import kotlinx.coroutines.*
import java.time.Duration
import java.time.InstantSource
import java.util.UUID

public class JobQueueRunner @Inject constructor(
    private val jobQueue: JobQueueInternal,
    private val jobsDAO: JobsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val jobHandlersProvider: JobHandlersProvider,
    private val config: JobQueueConfig,
    private val clock: InstantSource
) : Managed() {
    override suspend fun launch(): Unit = coroutineScope {
        val handlers = jobHandlersProvider.get()
        for (handler in handlers) {
            launch {
                SingleJobHandlerRunner(
                    jobQueue,
                    jobsDAO,
                    unitOfWorkFactory,
                    clock,
                    config,
                    handler
                ).run()
            }
        }
    }
}

public class SingleJobHandlerRunner<TJob : Job>(
    private val jobQueue: JobQueueInternal,
    private val jobsDAO: JobsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
    private val config: JobQueueConfig,
    private val jobHandler: JobHandler<TJob>,
) {
    private val logger = createLogger()

    private val pollWaitTime = Duration.ofSeconds(30)
    private val jobQueueId = jobHandler.queueId

    private val watchdogId = UUID.randomUUID().toString()

    public suspend fun run() {
        forever(logger) {
            val job = jobQueue.pollOne(jobQueueId, timeout = pollWaitTime)
            if (job != null) {
                withContext(NonCancellable) {
                    unitOfWorkFactory.use { uow ->
                        jobQueue.markAsInProgress(job, watchdogId, uow)
                    }

                    try {
                        handleJob(job)
                    } catch (e: Exception) {
                        handleFailure(job, e)
                    }
                }
            }
        }
    }

    private suspend fun handleJob(job: InternalJob<TJob>) = coroutineScope {
        val watchdogJob = launch {
            forever(logger) {
                delay(config.watchdogRefreshInterval.toMillis())
                if (!jobsDAO.updateWatchdog(job, clock.instant(), watchdogId)) {
                    this@coroutineScope.cancel()
                }
            }
        }

        unitOfWorkFactory.use { uow ->
            val context = createJobContext(uow)

            jobHandler.handle(job.job, context)
            jobQueue.markAsSuccessful(job, watchdogId, uow)
            watchdogJob.cancel()
        }
    }

    private fun createJobContext(uow: UnitOfWork): JobContextImpl {
        return JobContextImpl(uow)
    }

    private suspend fun handleFailure(job: InternalJob<TJob>, error: Exception) {
        if (error is JobOwnershipLostException) {
            return
        }

        unitOfWorkFactory.use { uow ->
            val failureContext = JobFailureContextImpl(
                numberOfFailures = jobQueue.getNumberOfFailuresFor(job) + 1,
                unitOfWork = uow
            )

            jobQueue.recordFailure(job, error, uow)

            val failureAction = jobHandler.onFailure(job.job, error, failureContext)
            when (failureAction) {
                is FailureAction.Reschedule -> {
                    jobQueue.rescheduleAt(job, failureAction.at, watchdogId, uow)
                }

                is FailureAction.RescheduleNow -> {
                    jobQueue.rescheduleAt(job, clock.instant(), watchdogId, uow)
                }

                FailureAction.GiveUp -> {
                    jobQueue.markAsFailed(job, error, watchdogId, uow)
                }
            }
        }
    }
}
