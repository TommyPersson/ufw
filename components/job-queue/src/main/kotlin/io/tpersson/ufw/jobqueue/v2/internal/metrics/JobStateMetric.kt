package io.tpersson.ufw.jobqueue.v2.internal.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.metrics.AbstractDatabaseQueueStateMetrics
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.v2.internal.DurableJobHandlersProvider
import io.tpersson.ufw.jobqueue.v2.internal.DurableJobsDatabaseQueueAdapterSettings
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
    measurementInterval = config.metricMeasurementInterval,
    adapterSettings = DurableJobsDatabaseQueueAdapterSettings,
) {
    override val queueIds: List<String> = jobHandlersProvider.get().map { it.jobDefinition.queueId }
}

