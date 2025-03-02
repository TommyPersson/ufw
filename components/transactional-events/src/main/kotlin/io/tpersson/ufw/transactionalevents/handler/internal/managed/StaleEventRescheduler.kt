package io.tpersson.ufw.transactionalevents.handler.internal.managed

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.transactionalevents.TransactionalEventsConfig
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import jakarta.inject.Inject
import java.time.InstantSource

public class StaleEventRescheduler @Inject constructor(
    private val eventQueueDAO: EventQueueDAO,
    private val clock: InstantSource,
    private val config: TransactionalEventsConfig
) : ManagedJob() {

    private val interval = config.stalenessDetectionInterval
    private val staleAfter = config.stalenessAge

    // For tests
    internal var hasFoundStaleEvents = false

    override suspend fun launch() {
        hasFoundStaleEvents = false

        forever(logger, interval = interval) {
            runOnce()
        }
    }

    private suspend fun runOnce() {
        val now = clock.instant()
        val staleIfWatchdogOlderThan = now - staleAfter

        val numRescheduled = eventQueueDAO.markStaleEventsAsScheduled(now, staleIfWatchdogOlderThan)
        if (numRescheduled > 0) {
            hasFoundStaleEvents = true
            logger.info("Rescheduled $numRescheduled stale events")
        }
    }
}