package io.tpersson.ufw.databasequeue.worker

public interface DatabaseQueueWorkerFactory {
    public fun create(
        queueId: String,
        handlersByType: Map<String, WorkItemHandler>,
    ): DatabaseQueueWorker
}