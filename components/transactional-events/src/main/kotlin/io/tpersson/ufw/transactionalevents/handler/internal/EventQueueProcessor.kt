package io.tpersson.ufw.transactionalevents.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.TransactionalEventsConfig
import io.tpersson.ufw.transactionalevents.handler.EventContextImpl
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.MDC
import java.time.Duration
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
    )
}

public class SingleEventQueueProcessor(
    private val handler: TransactionalEventHandler,
    private val eventQueue: EventQueue,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val objectMapper: ObjectMapper,
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

                    try {
                        handleEvent(event)
                    } catch (e: Exception) {
                        handleFailure(event, e)
                    }
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

    private suspend fun handleEvent(event: EventEntityData) = coroutineScope {
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

        val duration = measureTime {
            unitOfWorkFactory.use { uow ->
                val context = createEventContext(uow)

                handlerFunction.function.callSuspend(handlerFunction.instance, eventData, context)
                eventQueue.markAsSuccessful(event.eventId, watchdogId, uow)
                watchdogJob.cancel()
            }
        }

        //timer.record(duration.toJavaDuration())

        logger.info("Finished work on event: '${event.queueId}/${event.id}'. [Duration = ${duration.toString(DurationUnit.MILLISECONDS)}]")
    }

    private fun findHandlerMethod(event: EventEntityData): EventHandlerFunction {
        return handler.functions.get(event.topic to event.type) ?: error("not found")
    }

    private fun createEventContext(uow: UnitOfWork): EventContextImpl {
        return EventContextImpl(uow)
    }

    private fun handleFailure(event: EventEntityData, e: Exception) {
        TODO("Not yet implemented")
    }

    private val EventEntityData.eventId: EventId get() = EventId(id)
}
