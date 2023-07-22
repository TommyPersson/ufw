package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent
import io.tpersson.ufw.transactionalevents.handler.IncomingEventIngester
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class IncomingEventIngesterImpl @Inject constructor(
    private val eventHandlersProvider: EventHandlersProvider,
    private val eventQueueProvider: EventQueueProvider,
) : IncomingEventIngester {

    private val functionsByTopicAndType by Memoized({ eventHandlersProvider.get() }) { handlers ->
        handlers.flatMap { it.functions.toList() }.groupBy({ it.first }, { it.second })
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

