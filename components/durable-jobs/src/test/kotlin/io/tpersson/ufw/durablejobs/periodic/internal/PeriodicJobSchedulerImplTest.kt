package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.locks.DatabaseLock
import io.tpersson.ufw.database.locks.DatabaseLockHandle
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.worker.QueueStateChecker
import io.tpersson.ufw.durablejobs.*
import io.tpersson.ufw.durablejobs.internal.SimpleDurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.internal.jobDefinition2
import io.tpersson.ufw.durablejobs.periodic.PeriodicJob
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobStateData
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.mockito.kotlin.*
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal class PeriodicJobSchedulerImplTest {

    private lateinit var periodicJobSpecsProvider: PeriodicJobSpecsProviderImpl
    private lateinit var jobQueueMock: DurableJobQueue
    private lateinit var queueStateCheckerMock: QueueStateChecker
    private lateinit var databaseLocksMock: DatabaseLocks
    private lateinit var periodicJobsDAOFake: PeriodicJobsDAO
    private lateinit var unitOfWorkFactoryMock: UnitOfWorkFactory

    private lateinit var periodicJobScheduler: PeriodicJobSchedulerImpl

    private val testClock = TestClock(zone = ZoneId.of("UTC"))

    @BeforeEach
    fun setUp(): Unit {
        runBlocking {
            periodicJobSpecsProvider = PeriodicJobSpecsProviderImpl(
                jobHandlersProvider = SimpleDurableJobHandlersProvider(
                    mutableSetOf(
                        EveryMinuteJobHandler(),
                        EveryHourJobHandler(),
                        EveryDayJobHandler(),
                    )
                ),
            )

            jobQueueMock = mock<DurableJobQueue>()
            queueStateCheckerMock = mock<QueueStateChecker>()
            whenever(queueStateCheckerMock.isQueuePaused(any())).then { queuesArePaused }
            databaseLocksMock = mock<DatabaseLocks>()
            whenever(databaseLocksMock.create(any(), any())).then { DatabaseLockFake() }
            periodicJobsDAOFake = PeriodicJobsDAOFake()
            unitOfWorkFactoryMock = mock<UnitOfWorkFactory>()
            whenever(unitOfWorkFactoryMock.create()).then { UnitOfWorkFake() }

            periodicJobScheduler = PeriodicJobSchedulerImpl(
                periodicJobSpecsProvider = periodicJobSpecsProvider,
                jobQueue = jobQueueMock,
                queueStateChecker = queueStateCheckerMock,
                databaseLocks = databaseLocksMock,
                periodicJobsDAO = periodicJobsDAOFake,
                unitOfWorkFactory = unitOfWorkFactoryMock,
                appInfoProvider = AppInfoProvider.simple(),
                clock = testClock
            )

            databaseLockIsAvailable = true
            queuesArePaused = false

            periodicJobsDAOFake.debugTruncate()
        }
    }

    @Test
    fun `scheduleAnyPendingJobs - Shall schedule any jobs with their next attempt time passed`(): Unit = runBlocking {
        testClock.reset(Instant.parse("2022-02-02T13:00:05Z"))

        setupState(
            EveryMinuteJob::class to "2022-02-02T13:00:00Z",
            EveryHourJob::class to "2022-02-02T13:00:00Z",
            EveryDayJob::class to "2022-02-02T13:00:00Z",
        )

        periodicJobScheduler.scheduleAnyPendingJobs()

        verify(jobQueueMock).enqueue(argWhere { it is EveryMinuteJob && it.text == "every-minute" }, any(), any())
        verify(jobQueueMock).enqueue(argWhere { it is EveryHourJob && it.text == "every-hour" }, any(), any())
        verify(jobQueueMock).enqueue(argWhere { it is EveryDayJob && it.text == "every-day" }, any(), any())
    }

    @Test
    fun `scheduleAnyPendingJobs - Shall schedule any jobs only within their Cron window, if no next attempt time is available #1`(): Unit =
        runBlocking {
            testClock.reset(Instant.parse("2022-02-02T13:00:05Z"))

            setupState(
                EveryMinuteJob::class to null,
                EveryHourJob::class to null,
                EveryDayJob::class to null,
            )

            periodicJobScheduler.scheduleAnyPendingJobs()

            verify(jobQueueMock).enqueue(argWhere { it is EveryMinuteJob && it.text == "every-minute" }, any(), any())
            verify(jobQueueMock).enqueue(argWhere { it is EveryHourJob && it.text == "every-hour" }, any(), any())
            verify(jobQueueMock).enqueue(argWhere { it is EveryDayJob && it.text == "every-day" }, any(), any())
        }

    @Test
    fun `scheduleAnyPendingJobs - Shall schedule any jobs only within their Cron window, if no next attempt time is available #2`(): Unit =
        runBlocking {
            testClock.reset(Instant.parse("2022-02-02T13:02:05Z"))

            setupState(
                EveryMinuteJob::class to null,
                EveryHourJob::class to null,
                EveryDayJob::class to null,
            )

            periodicJobScheduler.scheduleAnyPendingJobs()

            verify(jobQueueMock, times(1)).enqueue(
                argWhere { it is EveryMinuteJob && it.text == "every-minute" },
                any(),
                any()
            )
            verify(jobQueueMock, times(0)).enqueue(
                argWhere { it is EveryHourJob && it.text == "every-hour" },
                any(),
                any()
            )
            verify(jobQueueMock, times(0)).enqueue(
                argWhere { it is EveryDayJob && it.text == "every-day" },
                any(),
                any()
            )
        }

    @Test
    fun `scheduleAnyPendingJobs - Shall update the next scheduling attempt time`(): Unit = runBlocking {
        val now = Instant.parse("2022-02-02T13:00:05Z")
        testClock.reset(now)

        setupState(
            EveryMinuteJob::class to "2022-02-02T13:00:00Z",
            EveryHourJob::class to "2022-02-02T13:00:00Z",
            EveryDayJob::class to "2022-02-02T13:00:00Z",
        )

        periodicJobScheduler.scheduleAnyPendingJobs()

        assertThat(getStateFor(EveryMinuteJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T13:01:00Z"))
        assertThat(getStateFor(EveryHourJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T14:00:00Z"))
        assertThat(getStateFor(EveryDayJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-03T13:00:00Z"))

        assertThat(getStateFor(EveryMinuteJob::class)?.lastSchedulingAttempt).isEqualTo(now)
        assertThat(getStateFor(EveryHourJob::class)?.lastSchedulingAttempt).isEqualTo(now)
        assertThat(getStateFor(EveryDayJob::class)?.lastSchedulingAttempt).isEqualTo(now)
    }

    @Test
    fun `scheduleAnyPendingJobs - Shall not schedule any jobs without their next attempt time passed`(): Unit =
        runBlocking {
            testClock.reset(Instant.parse("2022-02-02T12:59:55Z"))

            setupState(
                EveryMinuteJob::class to "2022-02-02T13:00:00Z",
                EveryHourJob::class to "2022-02-02T13:00:00Z",
                EveryDayJob::class to "2022-02-02T13:00:00Z",
            )

            periodicJobScheduler.scheduleAnyPendingJobs()

            verify(jobQueueMock, never()).enqueue(
                argWhere { it is EveryMinuteJob && it.text == "every-minute" },
                any(),
                any()
            )
            verify(jobQueueMock, never()).enqueue(
                argWhere { it is EveryHourJob && it.text == "every-hour" },
                any(),
                any()
            )
            verify(jobQueueMock, never()).enqueue(
                argWhere { it is EveryDayJob && it.text == "every-day" },
                any(),
                any()
            )
        }

    @Test
    fun `scheduleAnyPendingJobs - Shall not update the next scheduling attempt time if no attempt was intended`(): Unit =
        runBlocking {
            val now = Instant.parse("2022-02-02T12:59:55Z")
            testClock.reset(now)

            setupState(
                EveryMinuteJob::class to "2022-02-02T13:00:00Z",
                EveryHourJob::class to "2022-02-02T13:00:00Z",
                EveryDayJob::class to "2022-02-02T13:00:00Z",
            )

            periodicJobScheduler.scheduleAnyPendingJobs()

            assertThat(getStateFor(EveryMinuteJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T13:00:00Z"))
            assertThat(getStateFor(EveryHourJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T13:00:00Z"))
            assertThat(getStateFor(EveryDayJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T13:00:00Z"))

            assertThat(getStateFor(EveryMinuteJob::class)?.lastSchedulingAttempt).isNull()
            assertThat(getStateFor(EveryHourJob::class)?.lastSchedulingAttempt).isNull()
            assertThat(getStateFor(EveryDayJob::class)?.lastSchedulingAttempt).isNull()
        }

    @Test
    fun `scheduleAnyPendingJobs - Shall update the next scheduling attempt time without scheduling, if the queue is paused`(): Unit =
        runBlocking {
            testClock.reset(Instant.parse("2022-02-02T13:00:05Z"))

            queuesArePaused = true

            setupState(
                EveryMinuteJob::class to "2022-02-02T13:00:00Z",
                EveryHourJob::class to "2022-02-02T13:00:00Z",
                EveryDayJob::class to "2022-02-02T13:00:00Z",
            )

            periodicJobScheduler.scheduleAnyPendingJobs()

            verify(jobQueueMock, never()).enqueue(argWhere { it is EveryMinuteJob }, any(), any())
            assertThat(getStateFor(EveryMinuteJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T13:01:00Z"))
        }

    @Test
    fun `scheduleAnyPendingJobs - Shall not run if the database lock is unavailable`(): Unit = runBlocking {
        testClock.reset(Instant.parse("2022-02-02T13:00:05Z"))

        databaseLockIsAvailable = false

        setupState(
            EveryMinuteJob::class to "2022-02-02T13:00:00Z",
            EveryHourJob::class to "2022-02-02T13:00:00Z",
            EveryDayJob::class to "2022-02-02T13:00:00Z",
        )

        periodicJobScheduler.scheduleAnyPendingJobs()

        verify(jobQueueMock, never()).enqueue(argWhere { it is EveryMinuteJob }, any(), any())
        assertThat(getStateFor(EveryMinuteJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T13:00:00Z"))
    }


    @Test
    fun `scheduleJobNow - Shall schedule a job regardless of when the next attempt should be`(): Unit = runBlocking {
        val now = Instant.parse("2022-02-02T13:00:05Z")
        testClock.reset(now)

        databaseLockIsAvailable = false

        setupState(
            EveryMinuteJob::class to "2022-02-02T13:01:00Z",
            EveryHourJob::class to "2022-02-02T14:00:00Z",
            EveryDayJob::class to "2022-02-03T13:00:00Z",
        )

        periodicJobScheduler.scheduleJobNow(
            periodicJobSpec = periodicJobSpecsProvider.periodicJobSpecs.first { it.handler.jobDefinition.type == "EveryMinuteJob" },
            now = now
        )

        verify(jobQueueMock).enqueue(argWhere { it is EveryMinuteJob }, any(), any())
        assertThat(getStateFor(EveryMinuteJob::class)?.nextSchedulingAttempt).isEqualTo(Instant.parse("2022-02-02T13:01:00Z"))
        assertThat(getStateFor(EveryMinuteJob::class)?.lastSchedulingAttempt).isEqualTo(now)
    }

    private suspend fun setupState(vararg nextSchedulingAttemptsForJobClass: Pair<KClass<out DurableJob>, String?>) {
        val nextSchedulingAttemptsPerJobClass = nextSchedulingAttemptsForJobClass.toMap()

        val uow = UnitOfWorkFake()

        for ((jobClass, nextAttemptString) in nextSchedulingAttemptsPerJobClass) {
            val jobDefinition = jobClass.jobDefinition2
            val nextAttempt = nextAttemptString?.let { Instant.parse(it) }

            periodicJobsDAOFake.setSchedulingInfo(
                queueId = jobDefinition.queueId,
                jobType = jobDefinition.type,
                lastSchedulingAttempt = null,
                nextSchedulingAttempt = nextAttempt,
                unitOfWork = uow
            )
        }

        uow.commit()
    }

    private suspend fun getStateFor(jobClass: KClass<out DurableJob>): PeriodicJobStateData? {
        val jobDefinition = jobClass.jobDefinition2

        return periodicJobsDAOFake.get(
            queueId = jobDefinition.queueId,
            jobType = jobDefinition.type,
        )
    }

    @PeriodicJob(cronExpression = "* * * * *")
    class EveryMinuteJob(
        val text: String = "every-minute",
        override val id: DurableJobId = DurableJobId.new()
    ) : DurableJob

    class EveryMinuteJobHandler : DurableJobHandler<EveryMinuteJob> {
        override suspend fun handle(job: EveryMinuteJob, context: DurableJobContext) {
        }
    }

    @PeriodicJob(cronExpression = "0 * * * *")
    class EveryHourJob(
        val text: String = "every-hour",
        override val id: DurableJobId = DurableJobId.new()
    ) : DurableJob

    class EveryHourJobHandler : DurableJobHandler<EveryHourJob> {
        override suspend fun handle(job: EveryHourJob, context: DurableJobContext) {
        }
    }

    @PeriodicJob(cronExpression = "0 13 * * *")
    class EveryDayJob(
        val text: String = "every-day",
        override val id: DurableJobId = DurableJobId.new()
    ) : DurableJob

    class EveryDayJobHandler : DurableJobHandler<EveryDayJob> {
        override suspend fun handle(job: EveryDayJob, context: DurableJobContext) {
        }
    }

    class PeriodicJobsDAOFake : PeriodicJobsDAO {

        private val database = ConcurrentHashMap<Pair<DurableJobQueueId, String>, PeriodicJobStateData>()

        override suspend fun get(queueId: DurableJobQueueId, jobType: String): PeriodicJobStateData? {
            return database[queueId to jobType]
        }

        override suspend fun getAll(paginationOptions: PaginationOptions): PaginatedList<PeriodicJobStateData> {
            return PaginatedList(
                items = database.values.toList(),
                options = paginationOptions,
                hasMoreItems = false,
            )
        }

        override suspend fun setSchedulingInfo(
            queueId: DurableJobQueueId,
            jobType: String,
            nextSchedulingAttempt: Instant?,
            lastSchedulingAttempt: Instant?,
            unitOfWork: UnitOfWork
        ) {
            val dbState = database[queueId to jobType] ?: PeriodicJobStateData(queueId.value, jobType)

            unitOfWork.addPostCommitHook {
                database[queueId to jobType] = dbState.copy(
                    lastSchedulingAttempt = lastSchedulingAttempt,
                    nextSchedulingAttempt = nextSchedulingAttempt,
                )
            }
        }

        override suspend fun setExecutionInfo(
            queueId: DurableJobQueueId,
            jobType: String,
            state: WorkItemState?,
            stateChangeTimestamp: Instant?,
            unitOfWork: UnitOfWork
        ) {
            val dbState = database[queueId to jobType] ?: PeriodicJobStateData(queueId.value, jobType)

            unitOfWork.addPostCommitHook {
                database[queueId to jobType] = dbState.copy(
                    lastExecutionState = state?.dbOrdinal,
                    lastExecutionStateChangeTimestamp = stateChangeTimestamp,
                )
            }
        }

        override suspend fun debugTruncate() {
            database.clear()
        }
    }

    class UnitOfWorkFake : UnitOfWork {
        private val hooks = HashSet<suspend () -> Unit>()

        override fun add(minimumAffectedRows: Int, block: (Connection) -> PreparedStatement) {
        }

        override fun add(update: TypedUpdate, exceptionMapper: (Exception) -> Exception) {
        }

        override fun addPostCommitHook(block: suspend () -> Unit) {
            hooks += block
        }

        override suspend fun commit() {
            hooks.forEach { it() }
        }
    }

    class DatabaseLockFake : DatabaseLock {
        var locked = false

        override suspend fun tryAcquire(stealIfOlderThan: Duration?): DatabaseLockHandle? {
            if (databaseLockIsAvailable) {
                locked = true

                return object : DatabaseLockHandle {
                    override suspend fun refresh(): Boolean {
                        locked = true
                        return locked
                    }

                    override suspend fun release() {
                        locked = false
                    }
                }
            }

            return null
        }
    }

    companion object {
        var databaseLockIsAvailable = true
        var queuesArePaused = false
    }
}