package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.WorkItemHandler
import jakarta.inject.Inject

public class DatabaseQueueWorkerFactoryImpl @Inject constructor(
    private val processorFactory: SingleWorkItemProcessorFactory,
) : DatabaseQueueWorkerFactory {

    override fun create(
        queueId: String,
        handlersByType: Map<String, WorkItemHandler<*>>,
        adapterSettings: DatabaseQueueAdapterSettings
    ): DatabaseQueueWorker {
        return DatabaseQueueWorker(
            queueId = queueId,
            handlersByType = handlersByType,
            processorFactory = processorFactory,
            adapterSettings = adapterSettings,
        )
    }
}

