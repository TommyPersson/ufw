package io.tpersson.ufw.transactionalevents.handler.internal

import com.fasterxml.jackson.annotation.JsonTypeName
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.handler.EventHandler
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent
import io.tpersson.ufw.transactionalevents.handler.IncomingEventIngester
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Singleton
public class IncomingEventIngesterImpl @Inject constructor(
    private val eventHandlersProvider: EventHandlersProvider,
    private val eventQueueProvider: EventQueueProvider,
) : IncomingEventIngester {

    private val handlers = eventHandlersProvider.get()

    private val functionsByTopicAndType = mutableMapOf<Pair<String, String>, List<EventHandlerFunction>>()

    init {
        val functions = handlers.flatMap { it.functions.toList() }.groupBy({ it.first }, { it.second })

        functionsByTopicAndType.putAll(functions)
    }

    override suspend fun ingest(events: List<IncomingEvent>, unitOfWork: UnitOfWork) {
        for (event in events) {
            val handlerFunctions = functionsByTopicAndType[event.topic to event.type] ?: emptyList()

            for (function in handlerFunctions) {
                val queueId = function.instance.eventQueueId
                val queue = eventQueueProvider.get(queueId)

                queue.enqueue(event, unitOfWork = unitOfWork)
            }
        }
    }
}

