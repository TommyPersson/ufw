package io.tpersson.ufw.transactionalevents.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
import java.lang.reflect.InvocationTargetException
import java.time.Duration
import java.time.InstantSource
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.callSuspend
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toJavaDuration

@Singleton
public class EventQueueProcessor @Inject constructor(
    private val eventHandlersProvider: EventHandlersProvider,
    private val eventQueueProvider: EventQueueProvider,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
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
        meterRegistry = meterRegistry,
        clock = clock,
        config = config,
    )
}

public class SingleEventQueueProcessor(
    private val handler: TransactionalEventHandler,
    private val eventQueue: EventQueue,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    private val clock: InstantSource,
    private val config: TransactionalEventsConfig,
) {
    private val logger = createLogger()

    private val queueId = handler.eventQueueId
    private val watchdogId = UUID.randomUUID().toString()

    private val timeout: Duration = Duration.ofSeconds(30)

    private val timers = ConcurrentHashMap<String, Timer>()

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
        val (handlerFunction, eventData) = try {
            val handlerFunction = findHandlerMethod(event)
            val eventData = objectMapper.readValue(event.dataJson, handlerFunction.eventClass.java)
            handlerFunction to eventData

        } catch (t: Throwable) {
            handleFailure(event, null, null, t)
            return@coroutineScope
        }

        logger.info("Starting work on event: '${event.queueId}/${event.id}'")

        val watchdogJob = launch {
            forever(logger, interval = config.watchdogRefreshInterval) {
                if (!eventQueue.updateWatchdog(event.uid!!, watchdogId)) {
                    logger.warn("Unable to update watchdog for event '${event.queueId}/${event.id}'")
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

            getTimer(event.type).record(duration.toJavaDuration())

            logger.info(
                "Finished work on event: '${event.queueId}/${event.id}'. [Duration = ${
                    duration.toString(DurationUnit.MILLISECONDS)
                }]"
            )
        } catch (e: Exception) {
            val realError = if (e is InvocationTargetException) {
                e.targetException
            } else {
                e
            }

            handleFailure(event, eventData, handlerFunction, realError)
        }

        watchdogJob.cancel()
    }

    private fun findHandlerMethod(event: EventEntityData): EventHandlerFunction {
        return handler.functions[event.topic to event.type]
            ?: error("No handler found for event type '${event.type}' in handler '${handler::class.simpleName}'")
    }

    private fun createEventContext(uow: UnitOfWork): EventContextImpl {
        return EventContextImpl(uow)
    }

    private suspend fun handleFailure(
        event: EventEntityData,
        eventData: Event?,
        handlerFunction: EventHandlerFunction?,
        error: Throwable
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

            val failureAction = if (eventData != null && handlerFunction != null) {
                handlerFunction.instance.onFailure(eventData, error, failureContext)
            } else FailureAction.GiveUp

            when (failureAction) {
                is FailureAction.Reschedule -> {
                    logger.error(
                        "Failure during event: '${queueId}/${event.eventId}'. Rescheduling at ${failureAction.at}",
                        error
                    )
                    eventQueue.rescheduleAt(event.eventId, failureAction.at, watchdogId, uow)
                }

                is FailureAction.RescheduleNow -> {
                    logger.error("Failure during event: '${queueId}/${event.eventId}'. Rescheduling now.", error)
                    eventQueue.rescheduleAt(event.eventId, clock.instant(), watchdogId, uow)
                }

                FailureAction.GiveUp -> {
                    logger.error("Failure during event: '${queueId}/${event.eventId}'. Giving up.", error)
                    eventQueue.markAsFailed(event.eventId, watchdogId, uow)
                }
            }
        }
    }

    private fun getTimer(eventType: String): Timer {
        return timers.getOrPut(eventType) {
            Timer.builder("ufw.event_queue.duration.seconds")
                .tag("queueId", queueId.id)
                .tag("eventType", eventType)
                .publishPercentiles(0.5, 0.75, 0.90, 0.99, 0.999)
                .register(meterRegistry)
        }
    }

    private val EventEntityData.eventId: EventId get() = EventId(id)
}
