package io.tpersson.ufw.jobqueue.v2

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.jobqueue.IntegrationTests.TestInstantSource
import io.tpersson.ufw.jobqueue.dsl.jobQueue
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.test.suspendingUntil
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
        val workItemsDAO = ufw.databaseQueue.workItemsDAO

        init {
            ufw.database.migrator.run()
        }
    }

    @BeforeEach
    fun setup(): Unit = runBlocking {
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

        ufw.jobQueue.jobQueue.enqueue(MyJob(id = "the-id", greeting = "Hello"), unitOfWork = uow)

        uow.commit()

        await.suspendingUntil {
            workItemsDAO.getById("jq__MyJobs", "the-id")?.state == WorkItemState.SUCCESSFUL.dbOrdinal
        }
    }
}

@WithDurableJobDefinition(
    queueId = "MyJobs",
    type = "MyJob",
)
public data class MyJob(
    val greeting: String,
    override val id: JobId = JobId.new(),
) : DurableJob


public class MyJobHandler : DurableJobHandler<MyJob> {
    override suspend fun handle(job: MyJob, context: JobContext) {
        println("${job.greeting}, World!")
    }

    override suspend fun onFailure(job: MyJob, error: Exception, context: JobFailureContext): FailureAction {
        TODO("Not yet implemented")
    }
}