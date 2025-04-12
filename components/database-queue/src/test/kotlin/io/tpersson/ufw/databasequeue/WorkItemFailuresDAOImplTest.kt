package io.tpersson.ufw.databasequeue

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAOImpl
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
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
import java.util.UUID

internal class WorkItemFailuresDAOImplTest {

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
            core {
                clock = testClock
            }
            database {
                dataSource = HikariDataSource(config)
            }
            databaseQueue {
            }
        }

        val database = ufw.database.database
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory

        init {
            ufw.database.migrator.run()
        }
    }

    private lateinit var workItemsDAO: WorkItemsDAOImpl
    private lateinit var workItemFailuresDAO: WorkItemFailuresDAOImpl

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        workItemsDAO = WorkItemsDAOImpl(database, ufw.core.objectMapper)
        workItemFailuresDAO = WorkItemFailuresDAOImpl(database)

        workItemsDAO.debugTruncate()
        workItemFailuresDAO.debugTruncate()
    }

    @Test
    fun `insertFailure - Stores a failure`(): Unit = runBlocking {
        val now = Instant.now()

        val item = insertWorkItem(now)

        val error = Exception("uh no").fillInStackTrace()

        unitOfWorkFactory.use { uow ->
            workItemFailuresDAO.insertFailure(
                failure = WorkItemFailureDbEntity(
                  id = UUID.randomUUID().toString(),
                    itemUid = item.uid,
                    timestamp = now,
                    errorMessage = error.message!!,
                    errorType = error::class.simpleName!!,
                    errorStackTrace = error.stackTraceToString(),
                ),
                unitOfWork = uow
            )
        }

        val failures = workItemFailuresDAO.listFailuresForWorkItem(item.uid, PaginationOptions.DEFAULT).items
        val failure = failures.single()

        assertThat(failure.itemUid).isEqualTo(item.uid)
        assertThat(failure.errorType).isEqualTo("Exception")
        assertThat(failure.errorMessage).isEqualTo("uh no")
        assertThat(failure.errorStackTrace).isEqualTo(error.stackTraceToString())
    }

    @Test
    fun `misc - Failures are deleted if the parent work item is deleted`(): Unit = runBlocking {
        val now = Instant.now()

        val item = insertWorkItem(now)

        val error = Exception("uh no").fillInStackTrace()

        unitOfWorkFactory.use { uow ->
            workItemFailuresDAO.insertFailure(
                failure = WorkItemFailureDbEntity(
                    id = UUID.randomUUID().toString(),
                    itemUid = item.uid,
                    timestamp = now,
                    errorMessage = error.message!!,
                    errorType = error::class.simpleName!!,
                    errorStackTrace = error.stackTraceToString(),
                ),
                unitOfWork = uow
            )
        }

        workItemsDAO.debugTruncate()

        val failures = workItemFailuresDAO.listFailuresForWorkItem(item.uid, PaginationOptions.DEFAULT).items
        assertThat(failures).hasSize(0)
    }

    private suspend fun insertWorkItem(now: Instant): WorkItemDbEntity {
        workItemsDAO.debugInsert(
            WorkItemDbEntity(
                uid = 0,
                queueId = "queue-1",
                itemId = "item-1",
                type = "type-1",
                state = WorkItemState.FAILED.dbOrdinal,
                dataJson = "{}",
                metadataJson = "{}",
                concurrencyKey = null,
                createdAt = now,
                firstScheduledFor = now,
                nextScheduledFor = null,
                stateChangedAt = now,
                watchdogTimestamp = null,
                watchdogOwner = null,
                numFailures = 0,
                expiresAt = now.plus(Duration.ofDays(1)),
            )
        )

        return workItemsDAO.debugListAllItems().items.last()
    }
}