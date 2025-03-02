package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.WorkItemHandler

public interface DatabaseQueueWorkerFactory {
    public fun create(
        queueId: String,
        handlersByType: Map<String, WorkItemHandler<*>>,
        adapterSettings: DatabaseQueueAdapterSettings,
    ): DatabaseQueueWorker
}