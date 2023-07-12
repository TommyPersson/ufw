package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.core.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.InstantSource

public class StaleJobRescheduler @Inject constructor(
    private val jobRepository: JobRepository,
    private val clock: InstantSource,
    private val config: JobQueueConfig,
) : Managed() {

    private val logger = createLogger()

    private val delayTimeMs = config.stalenessDetectionInterval.toMillis()
    private val staleAfter = config.stalenessAge

    // For tests
    internal var hasFoundStaleJobs = false

    override suspend fun launch() {
        hasFoundStaleJobs = false

        forever(logger) {
            delay(delayTimeMs)

            runOnce()
        }
    }

    public suspend fun runOnce() {
        val now = clock.instant()
        val staleIfWatchdogOlderThan = now - staleAfter
        val numRescheduled = jobRepository.markStaleJobsAsScheduled(now, staleIfWatchdogOlderThan)
        if (numRescheduled > 0) {
            hasFoundStaleJobs = true
            logger.info("Rescheduled $numRescheduled stale jobs")
        }
    }
}