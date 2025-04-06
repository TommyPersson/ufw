package io.tpersson.ufw.durablejobs.periodic.internal

import com.cronutils.model.time.ExecutionTime
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.worker.QueueStateChecker
import io.tpersson.ufw.durablejobs.DurableJob
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobStateData
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.internal.toWorkItemQueueId
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.time.ZoneId
import java.util.UUID
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
    ) {
        val jobDefinition = periodicJobSpec.handler.jobDefinition
        val jobClass = jobDefinition.jobClass

        val job = try {
            jobClass.primaryConstructor?.callBy(emptyMap()) as DurableJob
        } catch (e: Exception) {
            logger.error("Cannot construct instance of class ${periodicJobSpec::class.simpleName}", e)
            return
        }

        val unitOfWork = unitOfWorkFactory.create()

        jobQueue.enqueue(job, unitOfWork)

        setState(
            periodicJobSpec = periodicJobSpec,
            state = PeriodicJobState(
                lastSchedulingAttempt = now,
                nextSchedulingAttempt = calculateNextAttemptTime(periodicJobSpec, now),
            ),
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()
    }

    private suspend fun trySchedulePeriodicJobs(now: Instant) {
        // TODO more efficient to lookup only db entries with a passed scheduling time

        for (periodicJobSpec in periodicJobSpecsProvider.periodicJobSpecs) {
            val state = getState(periodicJobSpec)

            if (state.nextSchedulingAttempt != null && state.nextSchedulingAttempt.isAfter(now)) {
                continue
            }

            val jobQueueId = periodicJobSpec.handler.jobDefinition.queueId

            val isJobPaused = queueStateChecker.isQueuePaused(jobQueueId.toWorkItemQueueId())
            if (isJobPaused) {
                val nextSchedulingAttempt = calculateNextAttemptTime(periodicJobSpec, now)

                val unitOfWork = unitOfWorkFactory.create()

                setState(
                    periodicJobSpec = periodicJobSpec,
                    state = state.copy(
                        nextSchedulingAttempt = nextSchedulingAttempt,
                    ),
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
            .nextExecution(now.atZone(ZoneId.of("UTC")))
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

    private suspend fun setState(
        periodicJobSpec: PeriodicJobSpec<*>,
        state: PeriodicJobState,
        unitOfWork: UnitOfWork
    ) {
        val jobDefinition = periodicJobSpec.handler.jobDefinition
        periodicJobsDAO.put(
            queueId = jobDefinition.queueId,
            jobType = jobDefinition.type,
            state = PeriodicJobStateData(
                queueId = jobDefinition.queueId.value,
                jobType = jobDefinition.type,
                lastSchedulingAttempt = state.lastSchedulingAttempt,
                nextSchedulingAttempt = state.nextSchedulingAttempt
            ),
            unitOfWork = unitOfWork
        )
    }
}