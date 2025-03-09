package io.tpersson.ufw.jobqueue.internal.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.metrics.AbstractDatabaseQueueStateMetrics
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.internal.DurableJobHandlersProvider
import io.tpersson.ufw.jobqueue.internal.DurableJobsDatabaseQueueAdapterSettings
import io.tpersson.ufw.jobqueue.internal.jobDefinition
import io.tpersson.ufw.jobqueue.internal.toWorkItemQueueId
import jakarta.inject.Inject

public class JobStateMetric @Inject constructor(
    meterRegistry: MeterRegistry,
    jobHandlersProvider: DurableJobHandlersProvider,
    workItemsDAO: WorkItemsDAO,
    config: JobQueueConfig,
) : AbstractDatabaseQueueStateMetrics(
    meterRegistry = meterRegistry,
    workItemsDAO = workItemsDAO,
    measurementInterval = config.metricMeasurementInterval,
    adapterSettings = DurableJobsDatabaseQueueAdapterSettings,
) {
    override val queueIds: List<WorkItemQueueId> =
        jobHandlersProvider.get().map { it.jobDefinition.queueId.toWorkItemQueueId() }
}

