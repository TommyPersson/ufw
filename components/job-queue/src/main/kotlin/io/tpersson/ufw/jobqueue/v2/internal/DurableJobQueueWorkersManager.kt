package io.tpersson.ufw.jobqueue.v2.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.worker.AbstractWorkQueueManager
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.jobqueue.v2.DurableJob
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import jakarta.inject.Inject
import jakarta.inject.Named


public class DurableJobQueueWorkersManager @Inject constructor(
    private val workerFactory: DatabaseQueueWorkerFactory,
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : AbstractWorkQueueManager(
    workerFactory = workerFactory,
    adapterSettings = DurableJobsDatabaseQueueAdapterSettings
) {
    protected override val handlersByTypeByQueueId: Map<WorkItemQueueId, Map<String, WorkItemHandler<*>>> =
        durableJobHandlersProvider.get()
            .groupBy { it.jobDefinition.queueId.toWorkItemQueueId() }
            .mapValues {
                it.value.associateBy(
                    keySelector = { handler -> handler.jobDefinition.type },
                    valueTransform = { handler ->
                        DurableJobHandlerAdapter(
                            handler.jobDefinition,
                            handler as DurableJobHandler<DurableJob>,
                            objectMapper
                        )
                    }
                )
            }
}