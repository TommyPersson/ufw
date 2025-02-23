package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import java.time.Instant

public interface WorkItemsDAO {

    public suspend fun scheduleNewItem(
        newItem: NewWorkItem,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun getById(
        queueId: String,
        itemId: String,
    ): WorkItemDbEntity?

    // TODO pagination
    public suspend fun listAllItems(): List<WorkItemDbEntity>

    public suspend fun takeNext(
        queueId: String,
        watchdogId: String,
        now: Instant
    ): WorkItemDbEntity?

    public suspend fun markInProgressItemAsSuccessful(
        queueId: String,
        itemId: String,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun markInProgressItemAsFailed(
        queueId: String,
        itemId: String,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun rescheduleInProgressItem(
        queueId: String,
        itemId: String,
        watchdogId: String,
        scheduleFor: Instant,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun manuallyRescheduleFailedItem(
        queueId: String,
        itemId: String,
        scheduleFor: Instant,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun forceCancelItem(
        queueId: String,
        itemId: String,
        expireAt: Instant,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun forcePauseItem(
        queueId: String,
        itemId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun pauseQueue(
        queueId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun refreshWatchdog(
        queueId: String,
        itemId: String,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun getEventsForItem(
        queueId: String,
        itemId: String
    ): List<WorkItemEvent>

    public suspend fun debugInsert(
        item: WorkItemDbEntity,
        unitOfWork: UnitOfWork? = null
    )

    public suspend fun debugTruncate()
}