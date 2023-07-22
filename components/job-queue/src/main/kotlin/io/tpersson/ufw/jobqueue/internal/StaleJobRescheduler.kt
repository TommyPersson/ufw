package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.InstantSource

public class StaleJobRescheduler @Inject constructor(
    private val jobsDAO: JobsDAO,
    private val clock: InstantSource,
    private val config: JobQueueConfig,
) : ManagedJob() {

    private val logger = createLogger()

    private val interval = config.stalenessDetectionInterval
    private val staleAfter = config.stalenessAge

    // For tests
    internal var hasFoundStaleJobs = false

    override suspend fun launch() {
        hasFoundStaleJobs = false

        forever(logger, interval = interval) {
            runOnce()
        }
    }

    private suspend fun runOnce() {
        val now = clock.instant()
        val staleIfWatchdogOlderThan = now - staleAfter
        val numRescheduled = jobsDAO.markStaleJobsAsScheduled(now, staleIfWatchdogOlderThan)
        if (numRescheduled > 0) {
            hasFoundStaleJobs = true
            logger.info("Rescheduled $numRescheduled stale jobs")
        }
    }
}