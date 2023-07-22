package io.tpersson.ufw.transactionalevents.handler.internal.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.transactionalevents.TransactionalEventsConfig
import io.tpersson.ufw.transactionalevents.handler.EventQueueId
import io.tpersson.ufw.transactionalevents.handler.EventState
import io.tpersson.ufw.transactionalevents.handler.internal.EventQueueProvider
import jakarta.inject.Inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

public class EventStateMetric @Inject constructor(
    private val meterRegistry: MeterRegistry,
    private val eventQueueProvider: EventQueueProvider,
    private val config: TransactionalEventsConfig,
) : ManagedJob() {

    private val logger = createLogger()

    private val gauges = ConcurrentHashMap<Pair<EventQueueId, EventState>, AtomicInteger>()

    override suspend fun launch() {
        forever(logger, interval = config.metricMeasurementInterval) {
            performMeasurement()
        }
    }

    private suspend fun performMeasurement() {
        for (queue in eventQueueProvider.all) {
            val queueStatistics = queue.getStatistics()

            getGauge(queue.id, EventState.Scheduled).set(queueStatistics.numScheduled)
            getGauge(queue.id, EventState.InProgress).set(queueStatistics.numInProgress)
            getGauge(queue.id, EventState.Successful).set(queueStatistics.numSuccessful)
            getGauge(queue.id, EventState.Failed).set(queueStatistics.numFailed)
        }
    }

    private fun getGauge(queueId: EventQueueId, state: EventState): AtomicInteger {
        return gauges.getOrPut(queueId to state) {
            meterRegistry.gauge(
                "ufw.event_queue.size",
                listOf(
                    Tag.of("queueId", queueId.id),
                    Tag.of("state", state.name)
                ),
                AtomicInteger(0)
            )!!
        }
    }
}