package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import jakarta.inject.Inject
import kotlinx.coroutines.*
import java.time.Duration
import java.time.InstantSource
import java.util.*

public class DatabaseQueueWorker @Inject constructor(
    private val queueId: String,
    private val handlersByType: Map<String, WorkItemHandler>,
    private val workItemsDAO: WorkItemsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
) {
    private val logger = createLogger<DatabaseQueueWorker>()

    private val fallbackPollInterval = Duration.ofSeconds(5)
    private val successfulItemExpirationDelay = Duration.ofDays(1)
    private val failedItemExpirationDelay = Duration.ofDays(14)

    private val watchdogId = UUID.randomUUID().toString()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    public fun start(): Job {
        return coroutineScope.launch {
            forever(logger, interval = fallbackPollInterval) {
                do {
                    val itemWasAvailable = processSingleItem(queueId, handlersByType)
                } while (itemWasAvailable)
            }
        }
    }

    private suspend fun processSingleItem(
        queueId: String,
        typeHandlerMappings: Map<String, WorkItemHandler>
    ): Boolean {
        // TODO setup MDC

        val workItem = workItemsDAO.takeNext(queueId, watchdogId, clock.instant())
            ?: return false

        val unitOfWork = unitOfWorkFactory.create()

        val handler = typeHandlerMappings[workItem.type]
        if (handler == null) {
            logger.warn("No handler found for type ${workItem.type}")
            return true
        } else {
            try {
                handler.handle(workItem)

                handleSuccess(workItem, unitOfWork)
            } catch (e: Exception) {
                println(e)
                handleFailure(workItem, unitOfWork)

            } finally {
                unitOfWork.commit()
            }

            return true
        }
    }

    private suspend fun handleSuccess(
        workItem: WorkItemDbEntity,
        unitOfWork: UnitOfWork
    ) {
        workItemsDAO.markInProgressItemAsSuccessful(
            queueId = workItem.queueId,
            itemId = workItem.itemId,
            expiresAt = clock.instant().plus(successfulItemExpirationDelay),
            watchdogId = watchdogId,
            now = clock.instant(),
            unitOfWork = unitOfWork,
        )
    }

    private suspend fun handleFailure(
        workItem: WorkItemDbEntity,
        unitOfWork: UnitOfWork
    ) {
        // TODO store failure
        workItemsDAO.markInProgressItemAsFailed(
            queueId = workItem.queueId,
            itemId = workItem.itemId,
            expiresAt = clock.instant().plus(failedItemExpirationDelay),
            watchdogId = watchdogId,
            now = clock.instant(),
            unitOfWork = unitOfWork,
        )
    }

    public data class Configuration( // Move to constructor?
        val queueId: String,
        val handlersByType: Map<String, WorkItemHandler>
    )
}