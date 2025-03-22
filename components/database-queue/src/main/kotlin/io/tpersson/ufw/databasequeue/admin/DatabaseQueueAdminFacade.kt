package io.tpersson.ufw.databasequeue.admin

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.databasequeue.*
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemQueueStatistics

public interface DatabaseQueueAdminFacade {
    public suspend fun getQueueStatistics(queueId: WorkItemQueueId): WorkItemQueueStatistics

    public suspend fun getWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId): WorkItemDbEntity?

    public suspend fun getWorkItems(
        queueId: WorkItemQueueId,
        state: WorkItemState,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemDbEntity>

    public suspend fun getWorkItemFailures(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemFailureDbEntity>

    public suspend fun deleteFailedJob(queueId: WorkItemQueueId, itemId: WorkItemId)

    public suspend fun rescheduleFailedJob(queueId: WorkItemQueueId, itemId: WorkItemId)

    public suspend fun cancelJob(queueId: WorkItemQueueId, itemId: WorkItemId)

    public suspend fun rescheduleAllFailedItems(queueId: WorkItemQueueId)

    public suspend fun deleteAllFailedItems(queueId: WorkItemQueueId)

    public suspend fun getQueueStatus(queueId: WorkItemQueueId): WorkQueueStatus

    public suspend fun pauseQueue(queueId: WorkItemQueueId)

    public suspend fun unpauseQueue(queueId: WorkItemQueueId)
}