package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Clock

public class DatabaseQueueHangedItemRescheduler @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val clock: Clock,
    private val config: DatabaseQueueConfig
): ManagedJob() {
    override suspend fun launch() {
        forever(logger, errorDelay = Duration.ofSeconds(5)) {
            runOnce()

            delay(config.expirationInterval.toMillis())
        }
    }

    public suspend fun runOnce() {
        val now = clock.instant()

        val numRescheduled = workItemsDAO.rescheduleAllHangedItems(
            rescheduleIfWatchdogOlderThan = now.minus(config.watchdogTimeout),
            scheduleFor = now,
            now = now,
        )

        if (numRescheduled > 0) {
            logger.info("Rescheduled $numRescheduled hanged work items")
        }
    }
}