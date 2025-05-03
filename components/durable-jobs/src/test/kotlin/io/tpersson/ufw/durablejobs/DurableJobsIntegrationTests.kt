package io.tpersson.ufw.durablejobs

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.WorkItemId
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.component.databaseQueue
import io.tpersson.ufw.durablejobs.component.durableJobs
import io.tpersson.ufw.durablejobs.component.installDurableJobs
import io.tpersson.ufw.durablejobs.internal.toWorkItemQueueId
import io.tpersson.ufw.managed.component.managed
import io.tpersson.ufw.test.TestClock
import io.tpersson.ufw.test.suspendingUntil
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName

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

        val testClock = TestClock()

        val ufw = UFW.build {
            installCore {
                clock = testClock
            }
            installDatabase {
                dataSource = HikariDataSource(config)
            }
            installDurableJobs {
                durableJobHandlers = setOf(MyJobHandler())
            }
        }

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

        ufw.durableJobs.jobQueue.enqueue(MyJob(id = DurableJobId("the-id"), greeting = "Hello"), unitOfWork = uow)

        uow.commit()

        await.suspendingUntil {
            workItemsDAO.getById(
                queueId = DurableJobQueueId("MyJobs").toWorkItemQueueId(),
                itemId = WorkItemId("the-id")
            )?.state == WorkItemState.SUCCESSFUL.dbOrdinal
        }
    }
}

@DurableJobTypeDefinition(
    queueId = "MyJobs",
    type = "MyJob",
)
public data class MyJob(
    val greeting: String,
    override val id: DurableJobId = DurableJobId.new(),
) : DurableJob


public class MyJobHandler : DurableJobHandler<MyJob> {
    override suspend fun handle(job: MyJob, context: DurableJobContext) {
        println("${job.greeting}, World!")
    }
}