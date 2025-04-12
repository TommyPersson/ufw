package io.tpersson.ufw.databasequeue.admin

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.*
import io.tpersson.ufw.databasequeue.internal.*
import jakarta.inject.Inject
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

public class DatabaseQueueAdminFacadeImpl @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val workItemFailuresDAO: WorkItemFailuresDAO,
    private val workQueuesDAO: WorkQueuesDAO,
    private val workQueue: WorkQueueInternal,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
) : DatabaseQueueAdminFacade {

    override suspend fun getQueueStatistics(queueId: WorkItemQueueId): WorkItemQueueStatistics {
        return workItemsDAO.getQueueStatistics(queueId)
    }

    override suspend fun getWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId): WorkItemDbEntity? {
        return workItemsDAO.getById(queueId, itemId)
    }

    override suspend fun getWorkItems(
        queueId: WorkItemQueueId,
        state: WorkItemState,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemDbEntity> {
        return workItemsDAO.listAllItems(queueId, state, paginationOptions)
    }

    override suspend fun getWorkItemFailures(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemFailureDbEntity> {
        val item = workItemsDAO.getById(queueId, itemId)
            ?: return PaginatedList.empty(paginationOptions)

        return workItemFailuresDAO.listFailuresForWorkItem(item.uid, paginationOptions)
    }

    override suspend fun getLatestCompletedItem(queueId: WorkItemQueueId): WorkItemDbEntity? {
        return workItemsDAO.getLatestCompletedItem(queueId)
    }

    override suspend fun deleteFailedWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId) {
        workItemsDAO.deleteFailedItem(queueId, itemId)
    }

    override suspend fun rescheduleFailedWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId) {
        val uow = unitOfWorkFactory.create()
        val now = clock.instant()

        val item = workItemsDAO.getById(queueId, itemId)
            ?: return

        workQueue.manuallyRescheduleFailedItem(
            item = item,
            scheduleFor = now,
            now = now,
            unitOfWork = uow
        )

        uow.commit()
    }

    override suspend fun cancelWorkItem(queueId: WorkItemQueueId, itemId: WorkItemId) {
        val uow = unitOfWorkFactory.create()
        val now = clock.instant()
        val expireAt = now.plus(Duration.ofDays(1)) // TODO config

        workItemsDAO.forceCancelItem(
            queueId = queueId,
            itemId = itemId,
            expireAt = expireAt,
            now = now,
            unitOfWork = uow
        )

        uow.commit()
    }

    override suspend fun rescheduleAllFailedWorkItems(queueId: WorkItemQueueId) {
        workItemsDAO.rescheduleAllFailedItems(queueId, clock.instant())
    }

    override suspend fun deleteAllFailedWorkItems(queueId: WorkItemQueueId) {
        workItemsDAO.deleteAllFailedItems(queueId)
    }

    override suspend fun getQueueStatus(queueId: WorkItemQueueId): WorkQueueStatus {
        return workQueuesDAO.getWorkQueue(queueId)?.let {
            WorkQueueStatus(
                state = WorkQueueState.valueOf(it.state),
                stateChangedAt = it.stateChangedAt,
            )
        } ?: WorkQueueStatus(
            state = WorkQueueState.ACTIVE,
            stateChangedAt = Instant.EPOCH
        )
    }

    public override suspend fun pauseQueue(queueId: WorkItemQueueId) {
        val state = getQueueStatus(queueId).state
        if (state == WorkQueueState.PAUSED) {
            return
        }

        workQueuesDAO.setWorkQueueState(
            queueId = queueId,
            state = WorkQueueState.PAUSED,
            now = clock.instant()
        )
    }

    override suspend fun unpauseQueue(queueId: WorkItemQueueId) {
        val state = getQueueStatus(queueId).state
        if (state != WorkQueueState.PAUSED) {
            return
        }

        workQueuesDAO.setWorkQueueState(
            queueId = queueId,
            state = WorkQueueState.ACTIVE,
            now = clock.instant()
        )
    }

}