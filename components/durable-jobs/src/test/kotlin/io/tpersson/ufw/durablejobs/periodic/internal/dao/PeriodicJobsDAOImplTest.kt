package io.tpersson.ufw.durablejobs.periodic.internal.dao

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import io.tpersson.ufw.durablejobs.dsl.durableJobs
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.test.TestInstantSource
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

        val testClock = TestInstantSource()

        val ufw = UFW.build {
            core {
                clock = testClock
            }
            managed {
            }
            database {
                dataSource = HikariDataSource(config)
            }
            databaseQueue {
            }
            durableJobs {
            }
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
    fun `put - Inserts data correctly`(): Unit = runBlocking {
        val queueId = DurableJobQueueId("unknown")
        val jobType = "unknown"

        val lastSchedulingAttempt = testClock.dbNow
        testClock.advance(Duration.ofMinutes(1))
        val nextSchedulingAttempt = testClock.dbNow

        val unitOfWork = unitOfWorkFactory.create()

        dao.put(
            queueId = queueId,
            jobType = jobType,
            state = PeriodicJobStateData(
                queueId = queueId.value,
                jobType = jobType,
                lastSchedulingAttempt = lastSchedulingAttempt,
                nextSchedulingAttempt = nextSchedulingAttempt,
            ),
            unitOfWork = unitOfWork,
        )

        unitOfWork.commit()

        val result = dao.get(queueId, jobType)

        assertThat(result?.lastSchedulingAttempt).isEqualTo(lastSchedulingAttempt)
        assertThat(result?.nextSchedulingAttempt).isEqualTo(nextSchedulingAttempt)
    }

    @Test
    fun `put - Updates existing state`(): Unit = runBlocking {
        val queueId = DurableJobQueueId("unknown")
        val jobType = "unknown"

        var lastSchedulingAttempt: Instant? = null
        var nextSchedulingAttempt: Instant? = null

        run {
            lastSchedulingAttempt = testClock.dbNow
            testClock.advance(Duration.ofMinutes(1))
            nextSchedulingAttempt = testClock.dbNow

            val unitOfWork = unitOfWorkFactory.create()

            dao.put(
                queueId = queueId,
                jobType = jobType,
                state = PeriodicJobStateData(
                    queueId = queueId.value,
                    jobType = jobType,
                    lastSchedulingAttempt = lastSchedulingAttempt,
                    nextSchedulingAttempt = nextSchedulingAttempt,
                ),
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

            dao.put(
                queueId = queueId,
                jobType = jobType,
                state = PeriodicJobStateData(
                    queueId = queueId.value,
                    jobType = jobType,
                    lastSchedulingAttempt = lastSchedulingAttempt,
                    nextSchedulingAttempt = nextSchedulingAttempt,
                ),
                unitOfWork = unitOfWork,
            )

            unitOfWork.commit()
        }


        val result = dao.get(queueId, jobType)

        assertThat(result?.lastSchedulingAttempt).isEqualTo(lastSchedulingAttempt)
        assertThat(result?.nextSchedulingAttempt).isEqualTo(nextSchedulingAttempt)
    }
}