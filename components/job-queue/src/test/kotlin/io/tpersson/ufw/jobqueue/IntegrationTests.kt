package io.tpersson.ufw.jobqueue

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.jobqueue.dsl.jobQueue
import io.tpersson.ufw.jobqueue.internal.JobQueueImpl
import io.tpersson.ufw.jobqueue.internal.JobQueueInternal
import io.tpersson.ufw.jobqueue.internal.JobRepositoryImpl
import io.tpersson.ufw.jobqueue.internal.StaleJobRescheduler
import io.tpersson.ufw.managed.dsl.managed
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.util.UUID

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
            managed {
            }
            database {
                dataSource = HikariDataSource(config)
            }
            jobQueue {
                handlers = setOf(TestJobHandler())

                configure {
                    stalenessDetectionInterval = Duration.ofMillis(50)
                    stalenessAge = Duration.ofMillis(90)
                    watchdogRefreshInterval = Duration.ofMillis(20)
                }
            }
        }

        val database = ufw.database.database
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory
        val jobQueue = ufw.jobQueue.jobQueue as JobQueueImpl
        val jobRepository = ufw.jobQueue.jobRepository
        val jobFailureRepository = ufw.jobQueue.jobFailureRepository
        val staleJobRescheduler = ufw.jobQueue.staleJobRescheduler

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

    @Test
    @Timeout(5)
    fun `Staleness - Stale jobs are automatically rescheduled`(): Unit = runBlocking {
        val jobId = JobId(UUID.randomUUID().toString())
        val queueId = JobQueueId(TestJob::class)

        database.update(
            JobRepositoryImpl.Queries.Updates.InsertJob(
                JobRepositoryImpl.JobData(
                    uid = 0,
                    id = jobId.value,
                    type = queueId.typeName,
                    state = JobState.InProgress.id,
                    json = """
                            {
                                "jobId": "$jobId",
                                "type": "${queueId.typeName}",
                                "greeting": "hello",
                                "shouldFail": false,
                                "numFailures": 0,
                                "numRetries": 0
                            }
                        """.trimIndent(),
                    createdAt = testClock.instant(),
                    scheduledFor = testClock.instant(),
                    stateChangedAt = testClock.instant(),
                    expireAt = null,
                    watchdogTimestamp = testClock.instant().minus(Duration.ofSeconds(1)),
                    watchdogOwner = "anyone"
                )
            )
        )

        do {
            // Wait for the rescheduler to do its thing
            delay(1)
            val job = jobRepository.getById(queueId, jobId)!!
        } while (job.state != JobState.Scheduled)

        do {
            // Speed up the test by triggering the queue manually
            delay(1)
            jobQueue.debugSignal(queueId)
            val job = jobRepository.getById(queueId, jobId)!!
        } while (job.state == JobState.Scheduled)

        // Then wait for the job to complete normally
        waitUntilQueueIsCompleted()

        val job = jobRepository.getById(queueId, jobId)!!

        assertThat(job.state).isEqualTo(JobState.Successful)
        assertThat(staleJobRescheduler.hasFoundStaleJobs).isTrue()
    }

    @Test
    @Timeout(5)
    fun `Staleness - An automatic watchdog refresher keeps long-running jobs from being stale`(): Unit = runBlocking {
        val testJob = TestJob(greeting = "Hello, World!", delayTime = Duration.ofSeconds(2))

        enqueueJob(testJob)

        waitUntilQueueIsCompleted()

        val job = jobRepository.getById(testJob.queueId, testJob.jobId)!!

        assertThat(job.state).isEqualTo(JobState.Successful)
        assertThat(staleJobRescheduler.hasFoundStaleJobs).isFalse()
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
        val delayTime: Duration? = null,
        override val jobId: JobId = JobId.new()
    ) : Job

    class TestJobHandler : JobHandler<TestJob> {
        override suspend fun handle(job: TestJob, context: JobContext) {
            if (job.shouldFail) {
                error("Job programmed to fail")
            }

            if (job.delayTime != null) {
                delay(job.delayTime.toMillis())
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