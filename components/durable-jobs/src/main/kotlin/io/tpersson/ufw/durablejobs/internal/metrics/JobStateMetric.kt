package io.tpersson.ufw.durablejobs.internal.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.configuration.DatabaseQueue
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.metrics.AbstractDatabaseQueueStateMetrics
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.DurableJobsDatabaseQueueAdapterSettings
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.internal.toWorkItemQueueId
import jakarta.inject.Inject

public class JobStateMetric @Inject constructor(
    meterRegistry: MeterRegistry,
    jobHandlersProvider: DurableJobHandlersProvider,
    workItemsDAO: WorkItemsDAO,
    configProvider: ConfigProvider,
) : AbstractDatabaseQueueStateMetrics(
    meterRegistry = meterRegistry,
    workItemsDAO = workItemsDAO,
    configProvider = configProvider,
    adapterSettings = DurableJobsDatabaseQueueAdapterSettings,
) {
    override val queueIds: List<WorkItemQueueId> =
        jobHandlersProvider.get().map { it.jobDefinition.queueId.toWorkItemQueueId() }
}

