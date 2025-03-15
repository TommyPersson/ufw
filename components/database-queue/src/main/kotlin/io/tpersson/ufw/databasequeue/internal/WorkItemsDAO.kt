package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.WorkItemId
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkItemState
import java.time.Instant

public interface WorkItemsDAO {

    public suspend fun scheduleNewItem(
        newItem: NewWorkItem,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun getById(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
    ): WorkItemDbEntity?

    public suspend fun listAllItems(
        state: WorkItemState? = null,
        paginationOptions: PaginationOptions = PaginationOptions.DEFAULT
    ): PaginatedList<WorkItemDbEntity>

    public suspend fun takeNext(
        queueId: WorkItemQueueId,
        watchdogId: String,
        now: Instant
    ): WorkItemDbEntity?

    public suspend fun markInProgressItemAsSuccessful(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun markInProgressItemAsFailed(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun rescheduleInProgressItem(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        watchdogId: String,
        scheduleFor: Instant,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun manuallyRescheduleFailedItem(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        scheduleFor: Instant,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun forceCancelItem(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        expireAt: Instant,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun forcePauseItem(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun pauseQueue(
        queueId: WorkItemQueueId,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun refreshWatchdog(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun rescheduleAllFailedItems(
        queueId: WorkItemQueueId,
        now: Instant,
    )

    public suspend fun getEventsForItem(
        queueId: WorkItemQueueId,
        itemId: WorkItemId
    ): List<WorkItemEvent>

    public suspend fun getQueueStatistics(
        queueId: WorkItemQueueId,
    ): WorkItemQueueStatistics

    public suspend fun deleteExpiredItems(now: Instant): Int

    public suspend fun debugInsert(
        item: WorkItemDbEntity,
        unitOfWork: UnitOfWork? = null
    )

    public suspend fun debugTruncate()


}