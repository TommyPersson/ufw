package io.tpersson.ufw.jobqueue

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.jobqueue.dsl.jobQueue
import io.tpersson.ufw.managed.dsl.managed
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
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

        val ufw = UFW.build {
            core {
                clock = testClock
            }
            database {
                dataSource = HikariDataSource(config)
            }
            jobQueue {
                handlers = setOf(TestJobHandler())
            }
            managed {
                instances = components.jobQueue.managedInstances
            }
        }

        val unitOfWorkFactory = ufw.database.unitOfWorkFactory
        val jobQueue = ufw.jobQueue.jobQueue
        val jobRepository = ufw.jobQueue.jobRepository
        val jobFailureRepository = ufw.jobQueue.jobFailureRepository

        init {
            ufw.database.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        ufw.managed.managedRunner.startAll()
    }

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        ufw.managed.managedRunner.stopAll()

        unitOfWorkFactory.use { uow ->
            jobRepository.debugTruncate(uow)
        }
    }

    @Test
    fun `Basic - Can run jobs`(): Unit = runBlocking {
        val testJob = TestJob(greeting = "Hello, World!")

        enqueueJob(testJob)

        waitUntilQueueIsCompleted()

        val job = jobRepository.getById(testJob.queueId, testJob.jobId)!!
        assertThat(job.state).isEqualTo(JobState.Successful)
    }

    @Test
    fun `Basic - Enqueueing the same job ID is idempotent`(): Unit = runBlocking {
        val testJob = TestJob(greeting = "Hello, World!", jobId = JobId("test-1"))
        val testJobDuplicate = TestJob(greeting = "Hello, World!", jobId = JobId("test-1"))

        enqueueJob(testJob)
        enqueueJob(testJobDuplicate)

        waitUntilQueueIsCompleted()

        val allJobs = jobRepository.debugGetAllJobs()

        assertThat(allJobs).hasSize(1)
    }

    @Test
    fun `Failures - Job state is set to 'Failed' on failure`(): Unit = runBlocking {
        val testJob = TestJob(greeting = "Hello, World!", shouldFail = true)

        enqueueJob(testJob)

        waitUntilQueueIsCompleted()

        val job = jobRepository.getById(testJob.queueId, testJob.jobId)!!
        assertThat(job.state).isEqualTo(JobState.Failed)
    }

    @Test
    fun `Failures - A JobFailure is recorded for each failure`(): Unit = runBlocking {
        val testJob = TestJob(greeting = "Hello, World!", shouldFail = true, numRetries = 3)

        enqueueJob(testJob)

        waitUntilQueueIsCompleted()

        val job = jobRepository.getById(testJob.queueId, testJob.jobId)!!
        val numFailures = jobFailureRepository.getNumberOfFailuresFor(job)
        val failures = jobFailureRepository.getLatestFor(job, limit = 10)

        assertThat(numFailures).isEqualTo(4)
        assertThat(failures).hasSize(4)
    }

    private suspend fun enqueueJob(testJob: TestJob) {
        unitOfWorkFactory.use { uow ->
            jobQueue.enqueue(testJob, uow)
        }
    }

    private suspend fun waitUntilQueueIsCompleted() {
        await.untilCallTo {
            runBlocking { jobRepository.debugGetAllJobs() }
        } matches {
            it!!.all { job -> job.state in setOf(JobState.Successful, JobState.Failed, JobState.Cancelled) }
        }
    }

    data class TestJob(
        val greeting: String,
        val shouldFail: Boolean = false,
        val numFailures: Int = Int.MAX_VALUE,
        val numRetries: Int = 0,
        override val jobId: JobId = JobId.new()
    ) : Job

    class TestJobHandler : JobHandler<TestJob> {
        override suspend fun handle(job: TestJob, context: JobContext) {
            if (job.shouldFail) {
                error("Job programmed to fail")
            }

            println(job.greeting)
        }

        override suspend fun onFailure(job: TestJob, error: Exception, context: JobFailureContext): FailureAction {
            return if (context.numberOfFailures > job.numRetries) {
                FailureAction.GiveUp
            } else {
                FailureAction.RescheduleNow
            }
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