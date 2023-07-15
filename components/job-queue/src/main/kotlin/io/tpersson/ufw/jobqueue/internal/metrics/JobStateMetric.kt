package io.tpersson.ufw.jobqueue.internal.metrics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.JobQueueId
import io.tpersson.ufw.jobqueue.internal.JobHandlersProvider
import io.tpersson.ufw.jobqueue.internal.JobsDAO
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

public class JobStateMetric @Inject constructor(
    @Named(NamedBindings.Meter) private val meter: Optional<Meter>,
    private val jobHandlersProvider: JobHandlersProvider,
    private val jobsDAO: JobsDAO,
    private val config: JobQueueConfig,
) : Managed() {

    private val jobHandlers = jobHandlersProvider.get()

    private val latestStatistics = ConcurrentHashMap<JobQueueId<*>, JobQueueStatistics<*>>()

    override suspend fun launch(): Unit {
        val meter = meter.getOrNull()
            ?: return

        val gauge = meter.gaugeBuilder("ufw.job_queue.size")
            .setDescription("The size of the job queue")
            .ofLongs()
            .buildWithCallback(::recordMeasurement)

        do  {
            readMeasurement()
            delay(config.metricMeasurementInterval.toMillis())
        } while (isActive)

        gauge.close()
    }

    private suspend fun readMeasurement() {
        for (handler in jobHandlers) {
            val queueId = handler.queueId

            val queueStatistics = jobsDAO.getQueueStatistics(queueId)

            latestStatistics[queueId] = queueStatistics
        }
    }

    private fun recordMeasurement(measurement: ObservableLongMeasurement): Unit {
        val stateKey = AttributeKey.stringKey("state")
        val queueIdKey = AttributeKey.stringKey("queueId")

        for ((queueId, stats) in latestStatistics) {
            measurement.record(stats.numScheduled.toLong(), Attributes.of(stateKey, "scheduled", queueIdKey, queueId.typeName))
            measurement.record(stats.numInProgress.toLong(), Attributes.of(stateKey, "in_progress", queueIdKey, queueId.typeName))
            measurement.record(stats.numSuccessful.toLong(), Attributes.of(stateKey, "successful", queueIdKey, queueId.typeName))
            measurement.record(stats.numFailed.toLong(), Attributes.of(stateKey, "failed", queueIdKey, queueId.typeName))
        }

    }
}