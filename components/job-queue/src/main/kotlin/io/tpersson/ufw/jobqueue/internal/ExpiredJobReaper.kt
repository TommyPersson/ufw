package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.InstantSource

public class ExpiredJobReaper @Inject constructor(
    private val jobsDAO: JobsDAO,
    private val clock: InstantSource,
    private val config: JobQueueConfig,
) : ManagedJob() {

    private val interval = config.expiredJobReapingInterval

    override suspend fun launch() {
        forever(logger) {
            delay(interval.toMillis())

            val now = clock.instant()
            val numDeleted = jobsDAO.deleteExpiredJobs(now)
            if (numDeleted > 0) {
                logger.info("Deleted $numDeleted expired jobs.")
            }
        }
    }
}