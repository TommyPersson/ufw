package io.tpersson.ufw.databasequeue

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.test.TestInstantSource
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class WorkItemsDAOImplTest {

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
            database {
                dataSource = HikariDataSource(config)
            }
        }

        val database = ufw.database.database

        init {
            // TODO move to component module
            Migrator.registerMigrationScript(
                componentName = "databasequeue",
                scriptLocation = "io/tpersson/ufw/databasequeue/migrations/postgres/liquibase.xml"
            )

            ufw.database.migrator.run()
        }
    }

    private lateinit var dao: WorkItemsDAOImpl

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        dao = WorkItemsDAOImpl(database)
        dao.debugTruncate()
    }

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        dao.debugTruncate()
    }

    @Test
    fun `takeNext - Takes a SCHEDULED item and moves it to IN_PROGRESS`(): Unit = runBlocking {
        insertItem(makeWorkItem(id = "testId", queueId = "testQueueId"))
        testClock.advance(Duration.ofMinutes(2))

        val now = testClock.instant().truncatedTo(ChronoUnit.MILLIS)

        run {
            val item = dao.takeNext(queueId = "testQueueId", watchdogId = "testWatchdog", now = now)

            assertThat(item?.state).isEqualTo(WorkItemState.IN_PROGRESS)
            assertThat(item?.stateChangedAt).isEqualTo(now)
            assertThat(item?.watchdogOwner).isEqualTo("testWatchdog")
            assertThat(item?.watchdogTimestamp).isEqualTo(now)
        }

        run {
            val item = dao.listAllItems().first { it.id == "testId" }

            assertThat(item.state).isEqualTo(WorkItemState.IN_PROGRESS)
            assertThat(item.stateChangedAt).isEqualTo(now)
            assertThat(item.watchdogOwner).isEqualTo("testWatchdog")
            assertThat(item.watchdogTimestamp).isEqualTo(now)
        }
    }

    @Test
    fun `takeNext - Shall not take an item with the same concurrency as an already IN_PROGRESS item`(): Unit =
        runBlocking {
            insertItem(makeWorkItem(id = "testId1", concurrencyKey = "concurrencyKey"))
            testClock.advance(Duration.ofMinutes(2))
            insertItem(makeWorkItem(id = "testId2", concurrencyKey = "concurrencyKey"))
            testClock.advance(Duration.ofMinutes(2))

            val now = testClock.instant().truncatedTo(ChronoUnit.MILLIS)

            run {
                val item = dao.takeNext(queueId = "testQueueId", watchdogId = "testWatchdog", now = now)

                assertThat(item?.id).isEqualTo("testId1")
            }

            run {
                val item = dao.takeNext(queueId = "testQueueId", watchdogId = "testWatchdog", now = now)

                assertThat(item).isNull()
            }
        }

    private suspend fun insertItem(item: WorkItemDbEntity) {
        val unitOfWork = ufw.database.unitOfWorkFactory.create()
        dao.insert(item, unitOfWork = unitOfWork)
        unitOfWork.commit()
    }

    private fun makeWorkItem(
        id: String = "testId",
        queueId: String = "testQueueId",
        state: Int = WorkItemState.SCHEDULED,
        concurrencyKey: String? = null,
        createdAt: Instant = testClock.instant(),
        firstScheduledFor: Instant = testClock.instant(),
        nextScheduledFor: Instant = testClock.instant(),
        stateChangedAt: Instant = testClock.instant(),
        watchdogTimestamp: Instant = testClock.instant(),
        watchdogOwner: String? = null,
        expiresAt: Instant? = null,
    ): WorkItemDbEntity {
        return WorkItemDbEntity(
            uid = 0L,
            id = id,
            queueId = queueId,
            type = "testType",
            state = state,
            dataJson = """{}""",
            metadataJson = """{}""",
            concurrencyKey = concurrencyKey,
            createdAt = createdAt,
            firstScheduledFor = firstScheduledFor,
            nextScheduledFor = nextScheduledFor,
            stateChangedAt = stateChangedAt,
            watchdogTimestamp = watchdogTimestamp,
            watchdogOwner = watchdogOwner,
            expiresAt = expiresAt,
        )
    }
}