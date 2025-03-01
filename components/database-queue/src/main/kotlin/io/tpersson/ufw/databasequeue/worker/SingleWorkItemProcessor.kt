package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.WorkItemHandler

public interface SingleWorkItemProcessor {
    public suspend fun processSingleItem(
        queueId: String,
        typeHandlerMappings: Map<String, WorkItemHandler<*>>
    ): Boolean
}