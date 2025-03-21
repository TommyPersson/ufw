package io.tpersson.ufw.databasequeue.worker

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.*
import io.tpersson.ufw.databasequeue.internal.*
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessor.ProcessingResult
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import java.time.Instant
import java.time.InstantSource
import java.util.*
import kotlin.time.measureTime
import kotlin.time.toJavaDuration

@Suppress("UNCHECKED_CAST")
public class SingleWorkItemProcessorImpl(
    private val watchdogId: String,
    private val workItemsDAO: WorkItemsDAO,
    private val workItemFailuresDAO: WorkItemFailuresDAO,
    private val queueStateChecker: QueueStateChecker,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val meterRegistry: MeterRegistry,
    private val clock: InstantSource,
    private val adapterSettings: DatabaseQueueAdapterSettings,
    private val config: DatabaseQueueConfig,
) : SingleWorkItemProcessor {

    private val logger = createLogger()

    private val timers = Collections.synchronizedMap(mutableMapOf<WorkItemQueueId, Timer>())

    override suspend fun processSingleItem(
        queueId: WorkItemQueueId,
        typeHandlerMappings: Map<String, WorkItemHandler<*>>
    ): ProcessingResult {
        if (queueStateChecker.isQueuePaused(queueId)) {
            return ProcessingResult.SKIPPED_QUEUE_PAUSED
        }

        val workItem = workItemsDAO.takeNext(queueId, watchdogId, clock.instant())
            ?: return ProcessingResult.SKIPPED_NO_ITEM_AVAILABLE

        MDC.put(adapterSettings.mdcQueueIdLabel, adapterSettings.convertQueueId(WorkItemQueueId(workItem.queueId)))
        MDC.put(adapterSettings.mdcItemIdLabel, workItem.itemId)
        MDC.put(adapterSettings.mdcItemTypeLabel, workItem.type)

        withContext(MDCContext()) {
            val timer = timers.getOrPut(queueId, { createTimer(queueId) })

            val duration = measureTime {
                invokeHandlerFor(workItem, typeHandlerMappings)
            }

            timer.record(duration.toJavaDuration())
        }

        return ProcessingResult.PROCESSED
    }

    private suspend fun invokeHandlerFor(
        workItem: WorkItemDbEntity,
        typeHandlerMappings: Map<String, WorkItemHandler<*>>,
    ) {
        val handler = typeHandlerMappings[workItem.type] as WorkItemHandler<Any>?
        if (handler == null) {
            logger.warn("No handler found for type ${workItem.type}")
            return
        }

        MDC.put(adapterSettings.mdcHandlerClassLabel, handler.handlerClassName)

        withContext(MDCContext()) {
            val successUnitOfWork = unitOfWorkFactory.create()
            val failureUnitOfWork = unitOfWorkFactory.create()

            val transformedItem = try {
                transformItem(handler, workItem)
            } catch (e: Exception) {
                handleFailure(
                    error = e,
                    workItem = workItem,
                    transformedItem = null,
                    handler = handler,
                    unitOfWork = failureUnitOfWork
                )

                failureUnitOfWork.commit()
                return@withContext
            }

            try {
                val context = createHandleContext(workItem, successUnitOfWork)

                handler.handle(transformedItem, context)

                handleSuccess(
                    workItem = workItem,
                    unitOfWork = successUnitOfWork
                )

                successUnitOfWork.commit()
            } catch (e: Exception) {
                handleFailure(
                    error = e,
                    workItem = workItem,
                    transformedItem = transformedItem,
                    handler = handler,
                    unitOfWork = failureUnitOfWork
                )
                failureUnitOfWork.commit()
            }
        }
    }

    private fun transformItem(
        handler: WorkItemHandler<Any>,
        workItem: WorkItemDbEntity
    ): Any {
        return try {
            handler.transformItem(workItem)
        } catch (e: Throwable) {
            throw UnableToTransformWorkItemException("Unable to transform work item of type: ${workItem.type}", e)
        }
    }

    private suspend fun handleSuccess(
        workItem: WorkItemDbEntity,
        unitOfWork: UnitOfWork
    ) {
        workItemsDAO.markInProgressItemAsSuccessful(
            queueId = WorkItemQueueId(workItem.queueId),
            itemId = WorkItemId(workItem.itemId),
            expiresAt = clock.instant().plus(config.successfulItemExpirationDelay),
            watchdogId = watchdogId,
            now = clock.instant(),
            unitOfWork = unitOfWork,
        )
    }

    private suspend fun handleFailure(
        error: Exception,
        workItem: WorkItemDbEntity,
        transformedItem: Any?,
        handler: WorkItemHandler<Any>,
        unitOfWork: UnitOfWork
    ) {
        val wasDeletedOrCancelled = isItemDeletedOrCancelled(
            queueId = WorkItemQueueId(workItem.queueId),
            itemId = WorkItemId(workItem.itemId)
        )

        if (wasDeletedOrCancelled) {
            // We can safely ignore errors caused by state changes
            return
        }

        val failureCount = workItem.numFailures + 1 // We include the current failure in the count

        val now = clock.instant()

        val context = object : WorkItemFailureContext {
            override val clock: InstantSource = this@SingleWorkItemProcessorImpl.clock
            override val timestamp: Instant = now
            override val failureCount: Int = failureCount
            override val unitOfWork: UnitOfWork = unitOfWork
        }

        val failureAction = if (transformedItem != null) {
            handler.onFailure(transformedItem, error, context)
        } else FailureAction.GiveUp

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
                queueId = WorkItemQueueId(workItem.queueId),
                itemId = WorkItemId(workItem.itemId),
                watchdogId = watchdogId,
                now = now,
                scheduleFor = rescheduleAt,
                unitOfWork = unitOfWork,
            )
        } else {
            workItemsDAO.markInProgressItemAsFailed(
                queueId = WorkItemQueueId(workItem.queueId),
                itemId = WorkItemId(workItem.itemId),
                expiresAt = now.plus(config.failedItemExpirationDelay),
                watchdogId = watchdogId,
                now = now,
                unitOfWork = unitOfWork,
            )
        }
    }

    private fun createHandleContext(
        workItem: WorkItemDbEntity,
        unitOfWork: UnitOfWork
    ) = object : WorkItemContext {
        override val clock: InstantSource = this@SingleWorkItemProcessorImpl.clock
        override val timestamp: Instant = this@SingleWorkItemProcessorImpl.clock.instant()
        override val failureCount: Int = workItem.numFailures
        override val unitOfWork: UnitOfWork = unitOfWork
    }

    private suspend fun isItemDeletedOrCancelled(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
    ): Boolean {
        val state = workItemsDAO.getById(queueId, itemId)
            ?.state
            ?.let { WorkItemState.fromDbOrdinal(it) }

        return state == null || state == WorkItemState.CANCELLED

    }

    private suspend fun getLatestItemState(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
    ): WorkItemState? {
        return workItemsDAO.getById(queueId, itemId)
            ?.state
            ?.let { WorkItemState.fromDbOrdinal(it) }
    }

    private fun createTimer(queueId: WorkItemQueueId): Timer {
        return Timer.builder(adapterSettings.metricsProcessingDurationMetricName)
            .tag("queueId", adapterSettings.convertQueueId(queueId))
            .publishPercentiles(0.5, 0.75, 0.90, 0.99, 0.999)
            .register(meterRegistry)
    }
}
