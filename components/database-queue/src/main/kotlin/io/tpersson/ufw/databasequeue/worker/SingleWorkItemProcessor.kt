package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.WorkItemFailureContext
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import java.time.Instant
import java.time.InstantSource
import java.util.*

@Suppress("UNCHECKED_CAST")
public class SingleWorkItemProcessor(
    private val watchdogId: String,
    private val workItemsDAO: WorkItemsDAO,
    private val workItemFailuresDAO: WorkItemFailuresDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
    private val config: DatabaseQueueConfig,
) {
    private val logger = createLogger()

    public suspend fun processSingleItem(
        queueId: String,
        typeHandlerMappings: Map<String, WorkItemHandler<*>>
    ): Boolean {
        // TODO extract to more easily tested class
        // TODO setup MDC

        val workItem = workItemsDAO.takeNext(queueId, watchdogId, clock.instant())
            ?: return false

        val unitOfWork = unitOfWorkFactory.create()

        val handler = typeHandlerMappings[workItem.type] as WorkItemHandler<Any>?
        if (handler == null) {
            logger.warn("No handler found for type ${workItem.type}")
        } else {
            val transformedItem = handler.transformItem(workItem) // TODO handle transformation errors

            try {
                handler.handle(transformedItem)

                handleSuccess(workItem, transformedItem, handler, unitOfWork)
            } catch (e: Exception) {
                handleFailure(e, workItem, transformedItem, handler, unitOfWork)
            } finally {
                unitOfWork.commit()
            }
        }

        return true
    }

    private suspend fun handleSuccess(
        workItem: WorkItemDbEntity,
        transformedItem: Any,
        handler: WorkItemHandler<Any>,
        unitOfWork: UnitOfWork
    ) {
        workItemsDAO.markInProgressItemAsSuccessful(
            queueId = workItem.queueId,
            itemId = workItem.itemId,
            expiresAt = clock.instant().plus(config.successfulItemExpirationDelay),
            watchdogId = watchdogId,
            now = clock.instant(),
            unitOfWork = unitOfWork,
        )
    }

    private suspend fun handleFailure(
        error: Exception,
        workItem: WorkItemDbEntity,
        transformedItem: Any,
        handler: WorkItemHandler<Any>,
        unitOfWork: UnitOfWork
    ) {
        val failureCount = workItem.numFailures + 1 // We include the current failure in the count

        val now = clock.instant()

        val context = object : WorkItemFailureContext {
            override val clock: InstantSource = this@SingleWorkItemProcessor.clock
            override val timestamp: Instant = now
            override val failureCount: Int = failureCount
        }

        val failureAction = handler.onFailure(transformedItem, error, context)

        val rescheduleAt: Instant? = when (failureAction) {
            FailureAction.GiveUp -> null
            is FailureAction.RescheduleAt -> failureAction.at
            FailureAction.RescheduleNow -> now
        }

        val failure = WorkItemFailureDbEntity(
            id = UUID.randomUUID().toString(), // TODO use UUIDv7 generator,
            itemUid = workItem.uid,
            timestamp = now,
            errorType = error.javaClass.simpleName ?: "<unknown>",
            errorMessage = error.message ?: "<no message>",
            errorStackTrace = error.stackTraceToString(),
        )

        workItemFailuresDAO.insertFailure(failure, unitOfWork = unitOfWork)

        if (rescheduleAt != null) {
            workItemsDAO.rescheduleInProgressItem(
                queueId = workItem.queueId,
                itemId = workItem.itemId,
                watchdogId = watchdogId,
                now = now,
                scheduleFor = rescheduleAt,
                unitOfWork = unitOfWork,
            )
        } else {
            workItemsDAO.markInProgressItemAsFailed(
                queueId = workItem.queueId,
                itemId = workItem.itemId,
                expiresAt = now.plus(config.failedItemExpirationDelay),
                watchdogId = watchdogId,
                now = now,
                unitOfWork = unitOfWork,
            )
        }
    }
}