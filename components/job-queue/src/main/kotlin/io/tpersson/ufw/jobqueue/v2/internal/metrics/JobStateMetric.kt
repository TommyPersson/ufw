package io.tpersson.ufw.jobqueue.v2.internal.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.v2.internal.DurableJobHandlersProvider
import io.tpersson.ufw.jobqueue.v2.internal.jobDefinition
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

public class JobStateMetric @Inject constructor(
    private val meterRegistry: MeterRegistry,
    private val jobHandlersProvider: DurableJobHandlersProvider,
    private val workItemsDAO: WorkItemsDAO,
    private val config: JobQueueConfig,
) : ManagedJob() {

    private val jobHandlers = jobHandlersProvider.get()

    private val gauges = ConcurrentHashMap<Pair<String, WorkItemState>, AtomicInteger>()

    override suspend fun launch(): Unit {
        do {
            performMeasurement()
            delay(config.metricMeasurementInterval.toMillis())
        } while (isActive)
    }

    private suspend fun performMeasurement() {
        for (handler in jobHandlers) {
            val queueId = handler.jobDefinition.queueId

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
                "ufw.job_queue.size",
                listOf(
                    Tag.of("queueId", queueId),
                    Tag.of("state", state.name)
                ),
                AtomicInteger(0)
            )!!
        }
    }
}