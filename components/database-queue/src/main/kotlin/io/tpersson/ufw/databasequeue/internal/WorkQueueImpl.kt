package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.extendOrCommit
import io.tpersson.ufw.databasequeue.*
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant

public class WorkQueueImpl @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
) : WorkQueueInternal {

    private val _stateChanges = MutableSharedFlow<WorkItemStateChange>()

    override val stateChanges: SharedFlow<WorkItemStateChange> = _stateChanges

    override suspend fun schedule(
        item: NewWorkItem,
        now: Instant,
        unitOfWork: UnitOfWork?
    ) {
        unitOfWorkFactory.extendOrCommit(unitOfWork) { uow ->
            workItemsDAO.scheduleNewItem(newItem = item, now = now, unitOfWork = uow)

            uow.addPostCommitHook {
                // TODO signal consumers

                _stateChanges.emit(
                    WorkItemStateChange(
                        queueId = item.queueId,
                        itemId = item.itemId,
                        itemType = item.type,
                        fromState = null,
                        toState = WorkItemState.SCHEDULED,
                        timestamp = now,
                    )
                )
            }
        }
    }

    override suspend fun takeNext(queueId: WorkItemQueueId, watchdogId: String, now: Instant): WorkItemDbEntity? {
        val item = workItemsDAO.takeNext(queueId, watchdogId, now)

        if (item != null) {
            notifyStateChange(
                item = item,
                fromState = WorkItemState.SCHEDULED,
                toState = WorkItemState.IN_PROGRESS,
                timestamp = now
            )
        }

        return item
    }

    override suspend fun markInProgressItemAsSuccessful(
        item: WorkItemDbEntity,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        workItemsDAO.markInProgressItemAsSuccessful(
            queueId = WorkItemQueueId(item.queueId),
            itemId = WorkItemId(item.itemId),
            expiresAt = expiresAt,
            watchdogId = watchdogId,
            now = now,
            unitOfWork = unitOfWork,
        )

        unitOfWork.addPostCommitHook {
            notifyStateChange(
                item = item,
                fromState = WorkItemState.IN_PROGRESS,
                toState = WorkItemState.SUCCESSFUL,
                timestamp = now,
            )
        }
    }

    override suspend fun rescheduleInProgressItem(
        item: WorkItemDbEntity,
        scheduleFor: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        workItemsDAO.rescheduleInProgressItem(
            queueId = WorkItemQueueId(item.queueId),
            itemId = WorkItemId(item.itemId),
            scheduleFor = scheduleFor,
            watchdogId = watchdogId,
            now = now,
            unitOfWork = unitOfWork,
        )

        unitOfWork.addPostCommitHook {
            notifyStateChange(
                item = item,
                fromState = WorkItemState.IN_PROGRESS,
                toState = WorkItemState.SCHEDULED,
                timestamp = now,
            )
        }
    }

    override suspend fun markInProgressItemAsFailed(
        item: WorkItemDbEntity,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        workItemsDAO.markInProgressItemAsFailed(
            queueId = WorkItemQueueId(item.queueId),
            itemId = WorkItemId(item.itemId),
            expiresAt = expiresAt,
            watchdogId = watchdogId,
            now = now,
            unitOfWork = unitOfWork,
        )

        unitOfWork.addPostCommitHook {
            notifyStateChange(
                item = item,
                fromState = WorkItemState.IN_PROGRESS,
                toState = WorkItemState.FAILED,
                timestamp = now,
            )
        }
    }

    private suspend fun notifyStateChange(
        item: WorkItemDbEntity,
        fromState: WorkItemState,
        toState: WorkItemState,
        timestamp: Instant,
    ) {
        _stateChanges.emit(
            WorkItemStateChange(
                queueId = WorkItemQueueId(item.queueId),
                itemId = WorkItemId(item.itemId),
                itemType = item.type,
                fromState = fromState,
                toState = toState,
                timestamp = timestamp,
            )
        )
    }
}
