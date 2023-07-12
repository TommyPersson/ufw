package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.core.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.jobqueue.internal.*
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import kotlinx.coroutines.*
import java.time.Duration
import java.time.InstantSource
import kotlin.reflect.KClass

public class JobQueueRunner @Inject constructor(
    private val jobQueue: JobQueueInternal,
    private val jobRepository: JobRepository,
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
                    jobRepository,
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
    private val jobRepository: JobRepository,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
    private val config: JobQueueConfig,
    private val jobHandler: JobHandler<TJob>,
) {
    private val logger = createLogger()

    private val pollWaitTime = Duration.ofSeconds(30)
    private val jobQueueId = JobQueueId(jobHandler.jobType)

    public suspend fun run() {
        forever(logger) {
            val job = jobQueue.pollOne(jobQueueId, timeout = pollWaitTime)
            if (job != null) {
                withContext(NonCancellable) {
                    unitOfWorkFactory.use { uow ->
                        jobQueue.markAsInProgress(job, uow)
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
                if (!jobRepository.updateWatchdog(job, clock.instant())) {
                    this@coroutineScope.cancel()
                }
            }
        }

        unitOfWorkFactory.use { uow ->
            val context = createJobContext(uow)

            jobHandler.handle(job.job, context)
            jobQueue.markAsSuccessful(job, uow)
            watchdogJob.cancel()
        }
    }

    private fun createJobContext(uow: UnitOfWork): JobContextImpl {
        return JobContextImpl(uow)
    }

    private suspend fun handleFailure(job: InternalJob<TJob>, error: Exception) {
        // TODO check for state conflicts
        unitOfWorkFactory.use { uow ->
            val failureContext = JobFailureContextImpl(
                numberOfFailures = jobQueue.getNumberOfFailuresFor(job) + 1,
                unitOfWork = uow
            )

            jobQueue.recordFailure(job, error, uow)

            val failureAction = jobHandler.onFailure(job.job, error, failureContext)
            when (failureAction) {
                is FailureAction.Reschedule -> {
                    jobQueue.rescheduleAt(job, failureAction.at, uow)
                }

                is FailureAction.RescheduleNow -> {
                    jobQueue.rescheduleAt(job, clock.instant(), uow)
                }

                FailureAction.GiveUp -> {
                    jobQueue.markAsFailed(job, error, uow)
                }
            }
        }
    }

    private val JobHandler<TJob>.jobType: KClass<TJob>
        get() = javaClass.kotlin
            .supertypes[0]
            .arguments[0]
            .type!!
            .classifier as KClass<TJob>
}
