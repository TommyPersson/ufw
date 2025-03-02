package io.tpersson.ufw.jobqueue.v2.internal.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.metrics.AbstractDatabaseQueueStateMetrics
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.v2.internal.DurableJobHandlersProvider
import io.tpersson.ufw.jobqueue.v2.internal.jobDefinition
import jakarta.inject.Inject

public class JobStateMetric @Inject constructor(
    meterRegistry: MeterRegistry,
    jobHandlersProvider: DurableJobHandlersProvider,
    workItemsDAO: WorkItemsDAO,
    config: JobQueueConfig,
) : AbstractDatabaseQueueStateMetrics(
    meterRegistry = meterRegistry,
    workItemsDAO = workItemsDAO,
    measurementInterval = config.metricMeasurementInterval
) {
    override val metricName: String = "ufw.job_queue.size"

    override val queueIds: List<String> = jobHandlersProvider.get().map { "jq__" + it.jobDefinition.queueId }

    override fun getTags(queueId: String, state: WorkItemState): List<Tag> {
        return listOf(
            Tag.of("queueId", queueId.substringAfter("jq__")),
            Tag.of("state", state.name)
        )
    }
}

