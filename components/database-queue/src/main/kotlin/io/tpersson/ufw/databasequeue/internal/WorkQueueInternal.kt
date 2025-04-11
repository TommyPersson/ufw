package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.WorkItemId
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkQueue
import java.time.Instant

public interface WorkQueueInternal : WorkQueue {
    public suspend fun takeNext(
        queueId: WorkItemQueueId,
        watchdogId: String,
        now: Instant
    ): WorkItemDbEntity? // TODO non-DbEntity type

    public suspend fun markInProgressItemAsSuccessful(
        item: WorkItemDbEntity,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork
    )

    public suspend fun rescheduleInProgressItem(
        item: WorkItemDbEntity,
        scheduleFor: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )

    public suspend fun markInProgressItemAsFailed(
        item: WorkItemDbEntity,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork,
    )
}