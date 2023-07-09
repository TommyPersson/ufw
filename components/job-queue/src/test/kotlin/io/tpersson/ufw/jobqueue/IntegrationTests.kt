package io.tpersson.ufw.jobqueue

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.unitofwork.use
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

internal class IntegrationTests {

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
        val dataSource = HikariDataSource(config)
        val coreComponent = CoreComponent.create(testClock)
        val databaseComponent = DatabaseComponent.create(dataSource)
        val connectionProvider = databaseComponent.connectionProvider
        val unitOfWorkFactory = databaseComponent.unitOfWorkFactory
        val jobQueueComponent = JobQueueComponent.create(
            coreComponent,
            databaseComponent,
            setOf(TestJobHandler())
        )
        val jobQueue = jobQueueComponent.jobQueue
        val jobRepository = jobQueueComponent.jobRepository

        init {
            databaseComponent.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        jobQueueComponent.jobQueueRunner.start()
    }

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        jobQueueComponent.jobQueueRunner.stop()
    }

    @Test
    fun `Basic`(): Unit = runBlocking {
        val testJob = TestJob(greeting = "Hello, World!")

        unitOfWorkFactory.use { uow ->
            jobQueue.enqueue(testJob, uow)
        }

        await.untilCallTo {
            jobRepository.getById(testJob.queueId, testJob.jobId)
        } matches {
            it?.state == JobState.Successful
        }
    }

    public data class TestJob(
        val greeting: String,
        override val jobId: JobId = JobId.new()
    ) : Job

    public class TestJobHandler : JobHandler<TestJob> {
        override suspend fun handle(job: TestJob, context: JobContext) {
            println(job.greeting)
        }

        override suspend fun onFailure(job: TestJob, error: Exception, context: JobFailureContext): FailureAction {
            TODO("Not yet implemented")
        }
    }

    class TestInstantSource : InstantSource {
        private var now = Instant.now()

        override fun instant(): Instant {
            return now
        }

        fun advance(duration: Duration) {
            now += duration
        }
    }
}