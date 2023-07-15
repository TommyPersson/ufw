package io.tpersson.ufw.jobqueue.internal.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.JobQueueId
import io.tpersson.ufw.jobqueue.JobState
import io.tpersson.ufw.jobqueue.internal.JobHandlersProvider
import io.tpersson.ufw.jobqueue.internal.JobsDAO
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

public class JobStateMetric @Inject constructor(
    private val meterRegistry: MeterRegistry,
    private val jobHandlersProvider: JobHandlersProvider,
    private val jobsDAO: JobsDAO,
    private val config: JobQueueConfig,
) : Managed() {

    private val jobHandlers = jobHandlersProvider.get()

    private val gauges = ConcurrentHashMap<Pair<JobQueueId<*>, JobState>, AtomicInteger>()

    override suspend fun launch(): Unit {
        do {
            performMeasurement()
            delay(config.metricMeasurementInterval.toMillis())
        } while (isActive)
    }

    private suspend fun performMeasurement() {
        for (handler in jobHandlers) {
            val queueId = handler.queueId

            val queueStatistics = jobsDAO.getQueueStatistics(queueId)

            getGauge(queueId, JobState.Scheduled).set(queueStatistics.numScheduled)
            getGauge(queueId, JobState.InProgress).set(queueStatistics.numInProgress)
            getGauge(queueId, JobState.Successful).set(queueStatistics.numSuccessful)
            getGauge(queueId, JobState.Failed).set(queueStatistics.numFailed)
        }
    }

    private fun getGauge(queueId: JobQueueId<*>, state: JobState): AtomicInteger {
        return gauges.getOrPut(queueId to state) {
            meterRegistry.gauge(
                "ufw.job_queue.size",
                listOf(
                    Tag.of("queueId", queueId.typeName),
                    Tag.of("state", state.name)
                ),
                AtomicInteger(0)
            )!!
        }
    }
}