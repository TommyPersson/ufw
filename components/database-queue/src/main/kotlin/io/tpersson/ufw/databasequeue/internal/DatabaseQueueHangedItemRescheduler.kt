package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Clock

public class DatabaseQueueHangedItemRescheduler @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val clock: Clock,
    private val config: DatabaseQueueConfig
): ManagedPeriodicTask(
    interval = config.expirationInterval
) {
    public override suspend fun runOnce() {
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