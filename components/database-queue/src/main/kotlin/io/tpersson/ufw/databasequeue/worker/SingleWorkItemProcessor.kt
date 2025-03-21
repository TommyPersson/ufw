package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.WorkItemQueueId

public interface SingleWorkItemProcessor {
    public suspend fun processSingleItem(
        queueId: WorkItemQueueId,
        typeHandlerMappings: Map<String, WorkItemHandler<*>>
    ): ProcessingResult

    public enum class ProcessingResult {
        SKIPPED_NO_ITEM_AVAILABLE,
        SKIPPED_QUEUE_PAUSED,
        PROCESSED,
    }
}