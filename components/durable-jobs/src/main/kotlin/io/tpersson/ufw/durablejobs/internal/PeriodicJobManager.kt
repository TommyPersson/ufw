package io.tpersson.ufw.durablejobs.internal

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.worker.QueueStateChecker
import io.tpersson.ufw.durablejobs.DurableJob
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.PeriodicJob
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

public class PeriodicJobManager @Inject constructor(
    private val jobHandlersProvider: DurableJobHandlersProvider,
    private val jobQueue: DurableJobQueue,
    private val queueStateChecker: QueueStateChecker,
    private val databaseLocks: DatabaseLocks,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
) : ManagedJob() {

    public val periodicJobSpecs: List<PeriodicJobSpec<*>> = jobHandlersProvider.get().mapNotNull {
        val annotation = it.jobDefinition.jobClass.findAnnotation<PeriodicJob>()
            ?: return@mapNotNull null

        PeriodicJobSpec(
            handler = it,
            cronExpression = annotation.cronExpression,
            cronInstance = cronParser.parse(annotation.cronExpression).validate()
        )
    }

    private val databaseLock =
        databaseLocks.create("PeriodicJobManagerLock", "apa") // TODO ApplicationInstanceIdProvider?

    private val database = ConcurrentHashMap<PeriodicJobSpec<*>, PeriodicJobState>()

    override suspend fun launch() {
        forever(logger) {
            runOnce()

            delay(1_000)
        }
    }

    private suspend fun runOnce() {
        val now = clock.instant()

        val lockHandle = databaseLock.tryAcquire(stealIfOlderThan = Duration.ofMinutes(1))
            ?: return

        logger.info("PeriodicJobManager: runOnce")

        try {
            trySchedulePeriodicJobs(now)
        } finally {
            lockHandle.release()
        }
    }

    private suspend fun trySchedulePeriodicJobs(now: Instant) {
        for (periodicJobSpec in periodicJobSpecs) {
            val state = database.getOrPut(periodicJobSpec) {
                PeriodicJobState()
            }

            if (state.nextEnqueue != null && state.nextEnqueue.isAfter(now)) {
                continue
            }

            val jobClass = periodicJobSpec.handler.jobDefinition.jobClass
            val jobQueueId = jobClass.jobDefinition2.queueId

            val isJobPaused = queueStateChecker.isQueuePaused(jobQueueId.toWorkItemQueueId())
            if (isJobPaused) {
                val nextEnqueue = calculateNextEnqueueTime(periodicJobSpec, now)
                database[periodicJobSpec] = state.copy(
                    nextEnqueue = nextEnqueue
                )

                logger.warn("The '${jobQueueId}' queue is paused. Skipping. Next attempt = ${nextEnqueue}")

                continue
            }

            val job = try {
                jobClass.primaryConstructor?.callBy(emptyMap()) as DurableJob
            } catch (e: Exception) {
                logger.error("Cannot construct instance of class ${periodicJobSpec::class.simpleName}", e)
                continue
            }

            val unitOfWork = unitOfWorkFactory.create()
            jobQueue.enqueue(job, unitOfWork)

            unitOfWork.addPostCommitHook {
                val nextEnqueue = calculateNextEnqueueTime(periodicJobSpec, now)

                database[periodicJobSpec] = state.copy(
                    lastEnqueue = now,
                    nextEnqueue = nextEnqueue
                )
                logger.info("[${periodicJobSpec.handler.jobDefinition.type}] => $nextEnqueue")
            }

            unitOfWork.commit()
        }
    }

    private fun calculateNextEnqueueTime(
        periodicJobSpec: PeriodicJobSpec<out DurableJob>,
        now: Instant
    ) = ExecutionTime.forCron(periodicJobSpec.cronInstance)
        .nextExecution(now.atZone(ZoneId.of("UTC")))
        .getOrNull()
        ?.toInstant()

    public companion object {
        private val cronParser = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX).let {
            CronParser(it)
        }
    }
}

public data class PeriodicJobState(
    val lastEnqueue: Instant? = null,
    val nextEnqueue: Instant? = null,
)

public data class PeriodicJobSpec<T : DurableJob>(
    val handler: DurableJobHandler<T>,
    val cronExpression: String,
    val cronInstance: Cron,
)