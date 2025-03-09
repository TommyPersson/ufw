package io.tpersson.ufw.databasequeue.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.convertQueueId
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.managed.ManagedJob
import kotlinx.coroutines.delay
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

public abstract class AbstractDatabaseQueueStateMetrics(
    private val meterRegistry: MeterRegistry,
    private val workItemsDAO: WorkItemsDAO,
    private val measurementInterval: Duration,
    private val adapterSettings: DatabaseQueueAdapterSettings,
) : ManagedJob() {

    protected abstract val queueIds: List<WorkItemQueueId>

    private val gauges = ConcurrentHashMap<Pair<WorkItemQueueId, WorkItemState>, AtomicInteger>()

    override suspend fun launch() {
        do {
            performMeasurement()
            delay(measurementInterval.toMillis())
        } while (isActive)
    }

    private suspend fun performMeasurement() {
        for (queueId in queueIds) {
            val queueStatistics = workItemsDAO.getQueueStatistics(queueId)

            getGauge(queueId, WorkItemState.SCHEDULED).set(queueStatistics.numScheduled)
            getGauge(queueId, WorkItemState.IN_PROGRESS).set(queueStatistics.numInProgress)
            getGauge(queueId, WorkItemState.SUCCESSFUL).set(queueStatistics.numSuccessful)
            getGauge(queueId, WorkItemState.FAILED).set(queueStatistics.numFailed)
        }
    }

    private fun getGauge(queueId: WorkItemQueueId, state: WorkItemState): AtomicInteger {
        return gauges.getOrPut(queueId to state) {
            meterRegistry.gauge(
                adapterSettings.metricsQueueStateMetricName,
                listOf(
                    Tag.of("queueId", adapterSettings.convertQueueId(queueId)),
                    Tag.of("state", state.name)
                ),
                AtomicInteger(0)
            )!!
        }
    }
}



