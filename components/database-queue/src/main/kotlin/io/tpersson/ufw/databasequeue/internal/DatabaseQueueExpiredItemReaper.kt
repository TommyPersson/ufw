package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Clock

public class DatabaseQueueExpiredItemReaper @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val clock: Clock,
    private val config: DatabaseQueueConfig
): ManagedPeriodicTask(
    interval = config.expirationInterval
) {
    public override suspend fun runOnce() {
        val numDeleted = workItemsDAO.deleteExpiredItems(now = clock.instant())

        if (numDeleted > 0) {
            logger.info("Deleted $numDeleted expired work items")
        }
    }
}