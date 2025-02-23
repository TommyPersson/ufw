package io.tpersson.ufw.jobqueue.v2

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.jobqueue.IntegrationTests.TestInstantSource
import io.tpersson.ufw.jobqueue.dsl.jobQueue
import io.tpersson.ufw.managed.dsl.managed
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.*

internal class DurableJobsIntegrationTests {


    private companion object {
        @JvmStatic
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15")).also {
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
            jobQueue {
                durableJobHandlers = setOf(MyJobHandler())

                configure {
                    stalenessDetectionInterval = Duration.ofMillis(50)
                    stalenessAge = Duration.ofMillis(90)
                    watchdogRefreshInterval = Duration.ofMillis(20)
                    successfulJobRetention = Duration.ofDays(1)
                    failedJobRetention = Duration.ofDays(2)
                    expiredJobReapingInterval = Duration.ofMillis(50)
                }
            }
        }

        val database = ufw.database.database
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory

        init {
            ufw.database.migrator.run()
        }
    }

    private lateinit var workItemsDAO: WorkItemsDAO

    @BeforeEach
    fun setup(): Unit = runBlocking {
        workItemsDAO = WorkItemsDAOImpl(
            database = ufw.database.database,
            objectMapper = ufw.core.objectMapper,
        )

        ufw.managed.managedRunner.startAll()

        workItemsDAO.debugTruncate()
    }

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        ufw.managed.managedRunner.stopAll()
    }

    @Test
    fun test1(): Unit = runBlocking {
        val uow = unitOfWorkFactory.create()

        workItemsDAO.scheduleNewItem(
            NewWorkItem(
                itemId = "the-id",
                queueId = "MyJobs",
                type = "MyJob",
                dataJson =  """{"id": "the-id", "greeting": "Hello"}""",
                metadataJson = "{}",
                scheduleFor = ufw.core.clock.instant(),
            ),
            unitOfWork = uow,
            now = ufw.core.clock.instant()
        )

        uow.commit()

        await.until {
            runBlocking {
                workItemsDAO.getById("MyJobs", "the-id")?.state == WorkItemState.SUCCESSFUL
            }
        }
    }
}

@WithDurableJobDefinition(
    queueId = "MyJobs",
    type = "MyJob",
)
public data class MyJob(
    val greeting: String,
    override val id: String = UUID.randomUUID().toString(),
) : DurableJob


public class MyJobHandler : DurableJobHandler<MyJob> {
    override suspend fun handle(job: MyJob) {
        println("${job.greeting}, World!")
    }

    override suspend fun onFailure(job: MyJob, error: Exception, context: JobFailureContext): FailureAction {
        TODO("Not yet implemented")
    }
}