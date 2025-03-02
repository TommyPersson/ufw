package io.tpersson.ufw.transactionalevents.handler.internal.managed

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.transactionalevents.TransactionalEventsConfig
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import jakarta.inject.Inject
import java.time.InstantSource

public class ExpiredEventReaper @Inject constructor(
    private val eventQueueDAO: EventQueueDAO,
    private val clock: InstantSource,
    private val config: TransactionalEventsConfig,
) : ManagedJob() {

    private val interval = config.expiredEventReapingInterval

    override suspend fun launch() {
        forever(logger, interval = interval) {
            val now = clock.instant()

            val numDeleted = eventQueueDAO.deleteExpiredEvents(now)
            if (numDeleted > 0) {
                logger.info("Deleted $numDeleted expired jobs.")
            }
        }
    }
}