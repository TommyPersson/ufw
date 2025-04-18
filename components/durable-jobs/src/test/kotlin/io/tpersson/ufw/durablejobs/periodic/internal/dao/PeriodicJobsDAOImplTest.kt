package io.tpersson.ufw.durablejobs.periodic.internal.dao

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import io.tpersson.ufw.durablejobs.component.installDurableJobs
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant

internal class PeriodicJobsDAOImplTest {
    companion object {
        @JvmStatic
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15"))
            .withReuse(true)
            .also {
                Startables.deepStart(it).join()
            }

        val config = HikariConfig().also {
            it.jdbcUrl = postgres.jdbcUrl
            it.username = postgres.username
            it.password = postgres.password
            it.maximumPoolSize = 5
            it.isAutoCommit = false
        }

        val testClock = TestClock()

        val ufw = UFW.build {
            installCore {
                clock = testClock
            }
            installDatabase {
                dataSource = HikariDataSource(config)
            }
            installDurableJobs()
        }

        val database = ufw.database.database
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory

        init {
            ufw.database.migrator.run()
        }
    }

    private lateinit var dao: PeriodicJobsDAOImpl

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        dao = PeriodicJobsDAOImpl(database)
        dao.debugTruncate()
    }

    @Test
    fun `get - Returns null for missing data`(): Unit = runBlocking {
        val queueId = DurableJobQueueId("unknown")
        val jobType = "unknown"

        val result = dao.get(queueId, jobType)

        assertThat(result).isNull()
    }

    @Test
    fun `getAll - Returns all data`(): Unit = runBlocking {
        unitOfWorkFactory.use { uow ->
            dao.setSchedulingInfo(
                queueId = DurableJobQueueId("1"),
                jobType = "2",
                lastSchedulingAttempt = Instant.now(),
                nextSchedulingAttempt = Instant.now(),
                unitOfWork = uow
            )

            dao.setExecutionInfo(
                queueId = DurableJobQueueId("3"),
                jobType = "4",
                state = WorkItemState.SUCCESSFUL,
                stateChangeTimestamp = Instant.now(),
                unitOfWork = uow
            )
        }

        val result = dao.getAll(PaginationOptions.DEFAULT)

        assertThat(result.items.firstOrNull { it.queueId == "1" && it.jobType == "2"  })
        assertThat(result.items.firstOrNull { it.queueId == "3" && it.jobType == "4"  })
    }

    @Test
    fun `setSchedulingInfo - Inserts data correctly`(): Unit = runBlocking {
        val queueId = DurableJobQueueId("unknown")
        val jobType = "unknown"

        val lastSchedulingAttempt = testClock.dbNow
        testClock.advance(Duration.ofMinutes(1))
        val nextSchedulingAttempt = testClock.dbNow

        val unitOfWork = unitOfWorkFactory.create()

        dao.setSchedulingInfo(
            queueId = queueId,
            jobType = jobType,
            lastSchedulingAttempt = lastSchedulingAttempt,
            nextSchedulingAttempt = nextSchedulingAttempt,
            unitOfWork = unitOfWork,
        )

        unitOfWork.commit()

        val result = dao.get(queueId, jobType)

        assertThat(result?.lastSchedulingAttempt).isEqualTo(lastSchedulingAttempt)
        assertThat(result?.nextSchedulingAttempt).isEqualTo(nextSchedulingAttempt)
    }

    @Test
    fun `setSchedulingInfo - Updates existing state`(): Unit = runBlocking {
        val queueId = DurableJobQueueId("unknown")
        val jobType = "unknown"

        var lastSchedulingAttempt: Instant?
        var nextSchedulingAttempt: Instant?

        run {
            lastSchedulingAttempt = testClock.dbNow
            testClock.advance(Duration.ofMinutes(1))
            nextSchedulingAttempt = testClock.dbNow

            val unitOfWork = unitOfWorkFactory.create()

            dao.setSchedulingInfo(
                queueId = queueId,
                jobType = jobType,
                lastSchedulingAttempt = lastSchedulingAttempt,
                nextSchedulingAttempt = nextSchedulingAttempt,
                unitOfWork = unitOfWork,
            )

            unitOfWork.commit()
        }

        run {
            testClock.advance(Duration.ofMinutes(1))
            lastSchedulingAttempt = testClock.dbNow
            testClock.advance(Duration.ofMinutes(1))
            nextSchedulingAttempt = testClock.dbNow

            val unitOfWork = unitOfWorkFactory.create()

            dao.setSchedulingInfo(
                queueId = queueId,
                jobType = jobType,
                lastSchedulingAttempt = lastSchedulingAttempt,
                nextSchedulingAttempt = nextSchedulingAttempt,
                unitOfWork = unitOfWork,
            )

            unitOfWork.commit()
        }


        val result = dao.get(queueId, jobType)

        assertThat(result?.lastSchedulingAttempt).isEqualTo(lastSchedulingAttempt)
        assertThat(result?.nextSchedulingAttempt).isEqualTo(nextSchedulingAttempt)
    }

    @Test
    fun `setExecutionInfo - Inserts data correctly`(): Unit = runBlocking {
        val queueId = DurableJobQueueId("unknown")
        val jobType = "unknown"

        val stateChangeTimestamp = testClock.dbNow

        val unitOfWork = unitOfWorkFactory.create()

        dao.setExecutionInfo(
            queueId = queueId,
            jobType = jobType,
            state = WorkItemState.SCHEDULED,
            stateChangeTimestamp = stateChangeTimestamp,
            unitOfWork = unitOfWork,
        )

        unitOfWork.commit()

        val result = dao.get(queueId, jobType)

        assertThat(result?.lastExecutionState).isEqualTo(WorkItemState.SCHEDULED.dbOrdinal)
        assertThat(result?.lastExecutionStateChangeTimestamp).isEqualTo(stateChangeTimestamp)
    }

    @Test
    fun `setExecutionInfo - Updates existing state`(): Unit = runBlocking {
        val queueId = DurableJobQueueId("unknown")
        val jobType = "unknown"

        var stateChangeTimestamp: Instant?

        run {
            stateChangeTimestamp = testClock.dbNow

            val unitOfWork = unitOfWorkFactory.create()

            dao.setExecutionInfo(
                queueId = queueId,
                jobType = jobType,
                state = WorkItemState.SCHEDULED,
                stateChangeTimestamp = stateChangeTimestamp,
                unitOfWork = unitOfWork,
            )

            unitOfWork.commit()
        }

        run {
            testClock.advance(Duration.ofMinutes(1))
            stateChangeTimestamp = testClock.dbNow

            val unitOfWork = unitOfWorkFactory.create()

            dao.setExecutionInfo(
                queueId = queueId,
                jobType = jobType,
                state = WorkItemState.IN_PROGRESS,
                stateChangeTimestamp = stateChangeTimestamp,
                unitOfWork = unitOfWork,
            )

            unitOfWork.commit()
        }


        val result = dao.get(queueId, jobType)

        assertThat(result?.lastExecutionState).isEqualTo(WorkItemState.IN_PROGRESS.dbOrdinal)
        assertThat(result?.lastExecutionStateChangeTimestamp).isEqualTo(stateChangeTimestamp)
    }
}