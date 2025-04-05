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

    public suspend fun getLatestCompletedItem(queueId: WorkItemQueueId): WorkItemDbEntity?

    public suspend fun deleteFailedWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId)

    public suspend fun rescheduleFailedWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId)

    public suspend fun cancelWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId)

    public suspend fun rescheduleAllFailedWorkItems(queueId: WorkItemQueueId)

    public suspend fun deleteAllFailedWorkItems(queueId: WorkItemQueueId)

    public suspend fun getQueueStatus(queueId: WorkItemQueueId): WorkQueueStatus

    public suspend fun pauseQueue(queueId: WorkItemQueueId)

    public suspend fun unpauseQueue(queueId: WorkItemQueueId)
}