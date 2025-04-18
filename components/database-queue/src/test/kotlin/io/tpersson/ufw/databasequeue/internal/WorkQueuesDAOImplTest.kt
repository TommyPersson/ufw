package io.tpersson.ufw.databasequeue.internal

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.builders.UFW
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.database.builder.database
import io.tpersson.ufw.database.builder.installDatabase
import io.tpersson.ufw.databasequeue.WorkQueueState
import io.tpersson.ufw.databasequeue.builder.installDatabaseQueue
import io.tpersson.ufw.databasequeue.toWorkItemQueueId
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.minutes

internal class WorkQueuesDAOImplTest {
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
            installDatabaseQueue()
        }

        val database = ufw.database.database
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory

        init {
            ufw.database.migrator.run()
        }
    }

    private lateinit var dao: WorkQueuesDAOImpl

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        dao = WorkQueuesDAOImpl(database)
        dao.debugTruncate()
    }

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        dao.debugTruncate()
    }

    @Test
    fun `getWorkQueue - Returns null for unknown queues`(): Unit = runBlocking {
        // Arrange
        val queueId = "unknown".toWorkItemQueueId()

        // Act
        val data = dao.getWorkQueue(queueId)

        // Assert
        assertThat(data).isNull()
    }

    @Test
    fun `setWorkQueueState - Inserts for first time`(): Unit = runBlocking {
        // Arrange
        val queueId = "test".toWorkItemQueueId()
        val now = testClock.dbNow

        // Act
        dao.setWorkQueueState(queueId, WorkQueueState.PAUSED, now)

        // Assert
        val data = dao.getWorkQueue(queueId)

        assertThat(data?.queueId).isEqualTo(queueId.value)
        assertThat(data?.state).isEqualTo(WorkQueueState.PAUSED.name)
        assertThat(data?.stateChangedAt).isEqualTo(now)
    }

    @Test
    fun `setWorkQueueState - Updates existing data`(): Unit = runBlocking {
        // Arrange
        val queueId = "test".toWorkItemQueueId()

        // Act
        dao.setWorkQueueState(queueId, WorkQueueState.PAUSED, testClock.dbNow)
        testClock.advance(1.minutes)
        dao.setWorkQueueState(queueId, WorkQueueState.ACTIVE, testClock.dbNow)

        // Assert
        val data = dao.getWorkQueue(queueId)

        assertThat(data?.queueId).isEqualTo(queueId.value)
        assertThat(data?.state).isEqualTo(WorkQueueState.ACTIVE.name)
        assertThat(data?.stateChangedAt).isEqualTo(testClock.dbNow)
    }
}