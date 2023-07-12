package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.core.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.InstantSource

public class StaleJobRescheduler @Inject constructor(
    private val jobRepository: JobRepository,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
    private val config: JobQueueConfig,
) : Managed() {

    private val logger = createLogger()

    private val delayTimeMs = config.stalenessDetectionInterval.toMillis()
    private val staleAfter = config.stalenessAge

    override suspend fun launch() {
        forever(logger) {
            delay(delayTimeMs)

            runOnce()
        }
    }

    public suspend fun runOnce() {
        unitOfWorkFactory.use { uow ->
            val now = clock.instant()
            val staleIfWatchdogOlderThan = now - staleAfter
            jobRepository.markStaleJobsAsScheduled(now, staleIfWatchdogOlderThan, uow)
        }
    }
}