package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkQueue
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class DatabaseQueueWorkerFactoryImpl @Inject constructor(
    private val processorFactory: SingleWorkItemProcessorFactory,
    private val workQueue: WorkQueue,
    private val configProvider: ConfigProvider
) : DatabaseQueueWorkerFactory {

    override fun create(
        queueId: WorkItemQueueId,
        handlersByType: Map<String, WorkItemHandler<*>>,
        adapterSettings: DatabaseQueueAdapterSettings
    ): DatabaseQueueWorker {
        return DatabaseQueueWorker(
            queueId = queueId,
            handlersByType = handlersByType,
            workQueue = workQueue,
            processorFactory = processorFactory,
            adapterSettings = adapterSettings,
            configProvider = configProvider,
        )
    }
}

