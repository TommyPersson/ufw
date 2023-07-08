package io.tpersson.ufw.jobqueue

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startable
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
        val jobQueueComponent = JobQueueComponent.create(coreComponent, databaseComponent, emptySet())
        val jobQueue = jobQueueComponent.jobQueue

        init {
            databaseComponent.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {

    }

    @Test
    fun `Basic`() = runBlocking {
        val testJob = TestJob(greeting = "Hello, World!")

        val unitOfWork = unitOfWorkFactory.create()
        jobQueue.enqueue(testJob, unitOfWork)

        unitOfWork.commit()
    }

    public data class TestJob(
        val greeting: String,
        override val jobId: JobId = JobId.new()
    ) : Job

    public class MyJobHandler : JobHandler<TestJob> {
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