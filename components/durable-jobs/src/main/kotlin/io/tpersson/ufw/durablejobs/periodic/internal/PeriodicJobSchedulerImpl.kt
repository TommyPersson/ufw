package io.tpersson.ufw.durablejobs.periodic.internal

import com.cronutils.model.time.ExecutionTime
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.worker.QueueStateChecker
import io.tpersson.ufw.durablejobs.DurableJob
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.internal.toWorkItemQueueId
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.time.ZoneId
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.full.primaryConstructor

public class PeriodicJobSchedulerImpl @Inject constructor(
    private val periodicJobSpecsProvider: PeriodicJobSpecsProvider,
    private val jobQueue: DurableJobQueue,
    private val queueStateChecker: QueueStateChecker,
    private val databaseLocks: DatabaseLocks,
    private val periodicJobsDAO: PeriodicJobsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
) : PeriodicJobScheduler {

    private val logger = createLogger()

    internal companion object {
        private val lockInstanceId = UUID.randomUUID().toString() // TODO ApplicationInstanceIdProvider?
    }

    private val databaseLock =
        databaseLocks.create("PeriodicJobManagerLock", lockInstanceId)

    override suspend fun scheduleAnyPendingJobs() {
        val now = clock.instant()

        val lockHandle = databaseLock.tryAcquire(stealIfOlderThan = Duration.ofMinutes(1))
            ?: return

        try {
            withContext(NonCancellable) {
                trySchedulePeriodicJobs(now)
            }
        } finally {
            lockHandle.release()
        }
    }

    override suspend fun scheduleJobNow(
        periodicJobSpec: PeriodicJobSpec<*>,
        now: Instant,
    ): DurableJobId {
        val jobDefinition = periodicJobSpec.handler.jobDefinition
        val jobClass = jobDefinition.jobClass

        val job = try {
            jobClass.primaryConstructor?.callBy(emptyMap()) as DurableJob
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot construct instance of class ${periodicJobSpec::class.simpleName}", e)
        }

        val unitOfWork = unitOfWorkFactory.create()

        jobQueue.enqueue(job, unitOfWork)

        setSchedulingInfo(
            periodicJobSpec = periodicJobSpec,
            lastSchedulingAttempt = now,
            nextSchedulingAttempt = calculateNextAttemptTime(periodicJobSpec, now),
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        return job.id
    }

    private suspend fun trySchedulePeriodicJobs(now: Instant) {
        // TODO more efficient to lookup all state at one time, instead of per spec

        for (periodicJobSpec in periodicJobSpecsProvider.periodicJobSpecs) {
            val state = getState(periodicJobSpec)

            if (state.nextSchedulingAttempt != null && state.nextSchedulingAttempt.isAfter(now)) {
                continue // TODO only schedule "new" specs in their window
            }

            val jobQueueId = periodicJobSpec.handler.jobDefinition.queueId

            val isJobPaused = queueStateChecker.isQueuePaused(jobQueueId.toWorkItemQueueId())
            if (isJobPaused) {
                val nextSchedulingAttempt = calculateNextAttemptTime(periodicJobSpec, now)

                val unitOfWork = unitOfWorkFactory.create()

                setSchedulingInfo(
                    periodicJobSpec = periodicJobSpec,
                    lastSchedulingAttempt = state.lastSchedulingAttempt,
                    nextSchedulingAttempt = nextSchedulingAttempt,
                    unitOfWork = unitOfWork
                )

                unitOfWork.commit()

                logger.warn("The '${jobQueueId}' queue is paused. Skipping. Next attempt = $nextSchedulingAttempt")

                continue
            }

            scheduleJobNow(periodicJobSpec, now)
        }
    }

    private fun calculateNextAttemptTime(
        periodicJobSpec: PeriodicJobSpec<out DurableJob>,
        now: Instant
    ): Instant? {
        return ExecutionTime.forCron(periodicJobSpec.cronInstance)
            .nextExecution(now.atZone(ZoneId.of("UTC"))) // TODO Allow the timezone to be configurable, or schedules involving specific hours will be wierd
            .getOrNull()
            ?.toInstant()
    }

    private suspend fun getState(spec: PeriodicJobSpec<*>): PeriodicJobState {
        return periodicJobsDAO.get(
            queueId = spec.handler.jobDefinition.queueId,
            jobType = spec.handler.jobDefinition.type
        )?.let {
            PeriodicJobState(
                lastSchedulingAttempt = it.lastSchedulingAttempt,
                nextSchedulingAttempt = it.nextSchedulingAttempt
            )
        } ?: PeriodicJobState()
    }

    private suspend fun setSchedulingInfo(
        periodicJobSpec: PeriodicJobSpec<*>,
        lastSchedulingAttempt: Instant?,
        nextSchedulingAttempt: Instant?,
        unitOfWork: UnitOfWork
    ) {
        val jobDefinition = periodicJobSpec.handler.jobDefinition

        periodicJobsDAO.setSchedulingInfo(
            queueId = jobDefinition.queueId,
            jobType = jobDefinition.type,
            lastSchedulingAttempt = lastSchedulingAttempt,
            nextSchedulingAttempt = nextSchedulingAttempt,
            unitOfWork = unitOfWork
        )
    }
}