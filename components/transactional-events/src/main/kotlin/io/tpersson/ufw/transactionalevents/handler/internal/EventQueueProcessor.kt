package io.tpersson.ufw.transactionalevents.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.TransactionalEventsConfig
import io.tpersson.ufw.transactionalevents.handler.FailureAction
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import io.tpersson.ufw.transactionalevents.handler.internal.exceptions.EventOwnershipLostException
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.MDC
import java.time.Duration
import java.time.InstantSource
import java.util.UUID
import kotlin.reflect.full.callSuspend
import kotlin.time.DurationUnit
import kotlin.time.measureTime

@Singleton
public class EventQueueProcessor @Inject constructor(
    private val eventHandlersProvider: EventHandlersProvider,
    private val eventQueueProvider: EventQueueProvider,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
    private val clock: InstantSource,
    private val config: TransactionalEventsConfig,
) : ManagedJob() {

    private val logger = createLogger()

    override suspend fun launch(): Unit = coroutineScope {
        val handlers = eventHandlersProvider.get()
        for (handler in handlers) {
            val eventQueue = eventQueueProvider.get(handler.eventQueueId)

            launch {
                logger.info("Starting work on queue: '${handler.eventQueueId}'")

                createProcessor(handler, eventQueue).run()
            }.invokeOnCompletion {
                logger.info("Stopping work on queue: '${handler.eventQueueId}'")
            }
        }
    }

    private fun createProcessor(
        handler: TransactionalEventHandler,
        eventQueue: EventQueue
    ) = SingleEventQueueProcessor(
        handler = handler,
        eventQueue = eventQueue,
        unitOfWorkFactory = unitOfWorkFactory,
        objectMapper = objectMapper,
        config = config,
        clock = clock
    )
}

public class SingleEventQueueProcessor(
    private val handler: TransactionalEventHandler,
    private val eventQueue: EventQueue,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val objectMapper: ObjectMapper,
    private val clock: InstantSource,
    private val config: TransactionalEventsConfig,
) {
    private val logger = createLogger()

    private val queueId = handler.eventQueueId
    private val watchdogId = UUID.randomUUID().toString()

    private val timeout: Duration = Duration.ofSeconds(30)

    public suspend fun run() {
        forever(logger) {
            val event = eventQueue.pollOne(timeout = timeout)
            if (event != null) {
                withEventContext(event) {
                    unitOfWorkFactory.use { uow ->
                        eventQueue.markAsInProgress(event.eventId, watchdogId, uow)
                    }

                    processEvent(event)
                }
            }
        }
    }

    private suspend fun withEventContext(event: EventEntityData, block: suspend CoroutineScope.() -> Unit) {
        MDC.put("queueId", queueId.id)
        MDC.put("eventId", event.id)
        MDC.put("eventType", event.type)

        return withContext(NonCancellable + MDCContext(), block)
    }

    private suspend fun processEvent(event: EventEntityData) = coroutineScope {
        val handlerFunction = findHandlerMethod(event)
        // TODO what if deserialization failure?
        val eventData = objectMapper.readValue(event.dataJson, handlerFunction.eventClass.java)

        logger.info("Starting work on event: '${event.queueId}/${event.id}'")

        val watchdogJob = launch {
            forever(logger) {
                delay(config.watchdogRefreshInterval.toMillis())
                if (!eventQueue.updateWatchdog(event.eventId, watchdogId)) {
                    this@coroutineScope.cancel()
                }
            }
        }

        try {
            val duration = measureTime {
                unitOfWorkFactory.use { uow ->
                    val context = createEventContext(uow)

                    handlerFunction.function.callSuspend(handlerFunction.instance, eventData, context)
                    eventQueue.markAsSuccessful(event.eventId, watchdogId, uow)
                }
            }

            //timer.record(duration.toJavaDuration())

            logger.info("Finished work on event: '${event.queueId}/${event.id}'. [Duration = ${duration.toString(DurationUnit.MILLISECONDS)}]")
        } catch (e: Exception) {
            handleFailure(event, eventData, handlerFunction, e)
        }

        watchdogJob.cancel()
    }

    private fun findHandlerMethod(event: EventEntityData): EventHandlerFunction {
        return handler.functions[event.topic to event.type] ?: error("not found")
    }

    private fun createEventContext(uow: UnitOfWork): EventContextImpl {
        return EventContextImpl(uow)
    }

    private suspend fun handleFailure(
        event: EventEntityData,
        eventData: Event,
        handlerFunction: EventHandlerFunction,
        error: Exception
    ) {
        if (error is EventOwnershipLostException) {
            return
        }

        unitOfWorkFactory.use { uow ->
            val failureContext = EventFailureContextImpl(
                numberOfFailures = eventQueue.getNumberOfFailuresFor(event.uid!!) + 1,
                unitOfWork = uow
            )

            eventQueue.recordFailure(event.uid, error, uow)

            val failureAction = handlerFunction.instance.onFailure(eventData, error, failureContext)
            when (failureAction) {
                is FailureAction.Reschedule -> {
                    logger.error("Failure during event: '${queueId}/${event.eventId}'. Rescheduling at ${failureAction.at}", error)
                    eventQueue.rescheduleAt(event.eventId, failureAction.at, watchdogId, uow)
                }

                is FailureAction.RescheduleNow -> {
                    logger.error("Failure during event: '${queueId}/${event.eventId}'. Rescheduling now.", error)
                    eventQueue.rescheduleAt(event.eventId, clock.instant(), watchdogId, uow)
                }

                FailureAction.GiveUp -> {
                    logger.error("Failure during event: '${queueId}/${event.eventId}'. Giving up.", error)
                    eventQueue.markAsFailed(event.eventId, error, watchdogId, uow)
                }
            }
        }
    }

    private val EventEntityData.eventId: EventId get() = EventId(id)
}
