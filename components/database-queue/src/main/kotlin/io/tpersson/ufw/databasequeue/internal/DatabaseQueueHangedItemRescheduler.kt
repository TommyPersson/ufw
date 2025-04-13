package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.databasequeue.configuration.DatabaseQueue
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Clock

public class DatabaseQueueHangedItemRescheduler @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val clock: Clock,
    private val configProvider: ConfigProvider
): ManagedPeriodicTask(
    interval = configProvider.get(Configs.DatabaseQueue.ItemReschedulingInterval)
) {
    private val watchdogTimeout = configProvider.get(Configs.DatabaseQueue.WatchdogTimeout)

    public override suspend fun runOnce() {
        val now = clock.instant()

        val numRescheduled = workItemsDAO.rescheduleAllHangedItems(
            rescheduleIfWatchdogOlderThan = now.minus(watchdogTimeout),
            scheduleFor = now,
            now = now,
        )

        if (numRescheduled > 0) {
            logger.info("Rescheduled $numRescheduled hanged work items")
        }
    }
}