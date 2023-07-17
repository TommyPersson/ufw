package io.tpersson.ufw.transactionalevents.handler.internal

import com.fasterxml.jackson.annotation.JsonTypeName
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.handler.*
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import jakarta.inject.Inject
import java.time.Instant
import java.time.InstantSource
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

public class IncomingEventIngesterImpl @Inject constructor(
    private val eventHandlersProvider: EventHandlersProvider,
    private val eventQueueDAO: EventQueueDAO,
    private val clock: InstantSource,
) : IncomingEventIngester {

    private val handlers = eventHandlersProvider.get()

    private val functionsByTopicAndType = mutableMapOf<Pair<String, String>, List<EventHandlerFunction>>()

    init {
        val functions = handlers.flatMap { handler ->
            handler::class.declaredMemberFunctions
                .filter { fn ->
                    fn.hasAnnotation<EventHandler>()
                }.map { fn ->
                    EventHandlerFunction(
                        topic = fn.findAnnotation<EventHandler>()!!.topic,
                        type = (fn.parameters[1].type.classifier as KClass<Event>).findAnnotation<JsonTypeName>()!!.value,
                        instance = handler,
                        function = fn,
                    )
                }
        }.groupBy { it.topic to it.type }

        functionsByTopicAndType.putAll(functions)
    }

    override fun ingest(events: List<IncomingEvent>, unitOfWork: UnitOfWork) {
        val now = clock.instant()

        for (event in events) {
            val handlerFunctions = functionsByTopicAndType[event.topic to event.type] ?: emptyList()

            for (function in handlerFunctions) {
                val eventEntity = createEventEntity(function, event, now)

                eventQueueDAO.insert(eventEntity, unitOfWork = unitOfWork)
            }
        }
    }

    private fun createEventEntity(
        function: EventHandlerFunction,
        event: IncomingEvent,
        now: Instant
    ): EventEntityData {
        return EventEntityData(
            queueId = function.instance.eventQueueId.id,
            id = event.id.value.toString(),
            topic = event.topic,
            type = event.type,
            dataJson = event.dataJson,
            ceDataJson = "{}",
            timestamp = event.timestamp,
            state = EventState.Scheduled.id,
            createdAt = now,
            scheduledFor = now,
            stateChangedAt = now,
            watchdogTimestamp = null,
            watchdogOwner = null,
            expireAt = null,
        )
    }
}

public data class EventHandlerFunction(
    val topic: String,
    val type: String,
    val instance: TransactionalEventHandler,
    val function: KFunction<*>,
)