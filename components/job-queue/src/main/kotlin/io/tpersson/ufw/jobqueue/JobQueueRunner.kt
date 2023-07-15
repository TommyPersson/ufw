package io.tpersson.ufw.jobqueue

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.tpersson.ufw.core.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.jobqueue.internal.*
import io.tpersson.ufw.jobqueue.internal.exceptions.JobOwnershipLostException
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.MDC
import java.time.Duration
import java.time.InstantSource
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toJavaDuration

public class JobQueueRunner @Inject constructor(
    private val jobQueue: JobQueueInternal,
    private val jobsDAO: JobsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val jobHandlersProvider: JobHandlersProvider,
    private val config: JobQueueConfig,
    private val clock: InstantSource,
    private val meterRegistry: MeterRegistry,
) : ManagedJob() {
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
                    meterRegistry,
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
    private val meterRegistry: MeterRegistry,
    private val jobHandler: JobHandler<TJob>,
) {
    private val logger = createLogger()

    private val pollWaitTime = Duration.ofSeconds(30)
    private val jobQueueId = jobHandler.queueId

    private val watchdogId = UUID.randomUUID().toString()

    private val timer = Timer.builder("ufw.job_queue.duration.seconds")
        .tag("queueId", jobQueueId.typeName)
        .publishPercentiles(0.5, 0.75, 0.90, 0.99, 0.999)
        .register(meterRegistry)

    public suspend fun run() {
        logger.info("Starting work on queue: '${jobQueueId.typeName}'")

        forever(logger) {
            val job = jobQueue.pollOne(jobQueueId, timeout = pollWaitTime)
            if (job != null) {

                withJobContext(job.job) {
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

    private suspend fun withJobContext(job: TJob, block: suspend CoroutineScope.() -> Unit) {
        MDC.put("queueId", jobQueueId.typeName)
        MDC.put("jobId", job.jobId.value)

        return withContext(NonCancellable + MDCContext(), block)
    }

    private suspend fun handleJob(job: InternalJob<TJob>) = coroutineScope {
        logger.info("Starting work on job: '${job.job.jobId}'")

        val watchdogJob = launch {
            forever(logger) {
                delay(config.watchdogRefreshInterval.toMillis())
                if (!jobsDAO.updateWatchdog(job, clock.instant(), watchdogId)) {
                    this@coroutineScope.cancel()
                }
            }
        }

        val duration = measureTime {
            unitOfWorkFactory.use { uow ->
                val context = createJobContext(uow)

                jobHandler.handle(job.job, context)
                jobQueue.markAsSuccessful(job, watchdogId, uow)
                watchdogJob.cancel()
            }
        }

        timer.record(duration.toJavaDuration())

        logger.info("Finished work on job: '${job.job.jobId}'. [Duration = ${duration.toString(DurationUnit.MILLISECONDS)}]")
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
                    logger.error("Failure during job: '${job.job.jobId}'. Rescheduling at ${failureAction.at}", error)
                    jobQueue.rescheduleAt(job, failureAction.at, watchdogId, uow)
                }

                is FailureAction.RescheduleNow -> {
                    logger.error("Failure during job: '${job.job.jobId}'. Rescheduling now.", error)
                    jobQueue.rescheduleAt(job, clock.instant(), watchdogId, uow)
                }

                FailureAction.GiveUp -> {
                    logger.error("Failure during job: '${job.job.jobId}'. Giving up.", error)
                    jobQueue.markAsFailed(job, error, watchdogId, uow)
                }
            }
        }
    }
}
