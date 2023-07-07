package io.tpersson.ufw.jobqueue

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactoryImpl
import io.tpersson.ufw.jobqueue.internal.JobQueueImpl
import io.tpersson.ufw.jobqueue.internal.JobRepositoryImpl
import kotlinx.coroutines.runBlocking
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
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15"))

        val config by lazy {
            HikariConfig().also {
                it.jdbcUrl = postgres.jdbcUrl
                it.username = postgres.username
                it.password = postgres.password
                it.maximumPoolSize = 5
                it.isAutoCommit = false
            }
        }
        val dataSource by lazy { HikariDataSource(config) }

        val connectionProvider by lazy { ConnectionProviderImpl(dataSource) }

        val unitOfWorkFactory by lazy { UnitOfWorkFactoryImpl(connectionProvider, DbModuleConfig.Default) }

        val testClock = TestInstantSource()

        val jobQueue: JobQueue by lazy {
            val config = JobQueueModuleConfig(
                pollWaitTime = Duration.ofMillis(50),
                defaultJobTimeout = Duration.ofSeconds(10),
                defaultJobRetention = Duration.ofSeconds(10),
            )

            val objectMapper = jacksonObjectMapper()

            JobQueue.create(config, DbModuleConfig.Default, testClock, connectionProvider, objectMapper)
        }

        init {
            runBlocking {
                Startables.deepStart(postgres).join()
            }
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