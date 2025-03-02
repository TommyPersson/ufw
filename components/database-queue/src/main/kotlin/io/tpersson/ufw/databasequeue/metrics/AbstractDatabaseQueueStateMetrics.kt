package io.tpersson.ufw.databasequeue.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.tpersson.ufw.databasequeue.WorkItemState
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
) : ManagedJob() {

    protected abstract val metricName: String

    protected abstract val queueIds: List<String>

    protected abstract fun getTags(queueId: String, state: WorkItemState): List<Tag>

    private val gauges = ConcurrentHashMap<Pair<String, WorkItemState>, AtomicInteger>()

    override suspend fun launch() {
        do {
            performMeasurement()
            delay(measurementInterval.toMillis())
        } while (isActive)
    }

    private suspend fun performMeasurement() {
        for (queueId in queueIds) {
            val queueStatistics = workItemsDAO.getQueueStatistics("jq__$queueId")

            getGauge(queueId, WorkItemState.SCHEDULED).set(queueStatistics.numScheduled)
            getGauge(queueId, WorkItemState.IN_PROGRESS).set(queueStatistics.numInProgress)
            getGauge(queueId, WorkItemState.SUCCESSFUL).set(queueStatistics.numSuccessful)
            getGauge(queueId, WorkItemState.FAILED).set(queueStatistics.numFailed)
        }
    }

    private fun getGauge(queueId: String, state: WorkItemState): AtomicInteger {
        return gauges.getOrPut(queueId to state) {
            meterRegistry.gauge(
                metricName,
                getTags(queueId, state),
                AtomicInteger(0)
            )!!
        }
    }
}