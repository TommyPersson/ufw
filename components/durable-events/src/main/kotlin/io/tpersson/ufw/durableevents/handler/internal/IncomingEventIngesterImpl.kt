package io.tpersson.ufw.durableevents.handler.internal

import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.durableevents.common.DurableEventQueueId
import io.tpersson.ufw.durableevents.common.IncomingEvent
import io.tpersson.ufw.durableevents.common.IncomingEventIngester
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock

@Singleton
public class IncomingEventIngesterImpl @Inject constructor(
    private val eventHandlersProvider: DurableEventHandlersProvider,
    private val workItemsDAO: WorkItemsDAO,
    private val clock: Clock,
) : IncomingEventIngester {

    private val queuesByTopicAndType: Map<Pair<String, String>, List<DurableEventQueueId>> by Memoized({ eventHandlersProvider.get() }) { handlers ->
        handlers.flatMap { it.findHandlerMethods() }.groupBy({ it.eventTopic to it.eventType }, { it.handler.queueId })
    }

    override suspend fun ingest(events: List<IncomingEvent>, unitOfWork: UnitOfWork) {
        for (event in events) {
            val queueIds = queuesByTopicAndType[event.topic to event.type] ?: emptyList()

            for (queueId in queueIds) {
                val workItem = createNewWorkItem(event, queueId)

                workItemsDAO.scheduleNewItem(workItem, clock.instant(), unitOfWork)
            }
        }
    }

    private fun createNewWorkItem(
        event: IncomingEvent,
        queueId: DurableEventQueueId
    ) = NewWorkItem(
        itemId = event.id.toWorkItemId(),
        queueId = queueId.toWorkItemQueueId(),
        type = event.type,
        metadataJson = "{}", // TODOm
        dataJson = event.dataJson,
        scheduleFor = clock.instant(),
    )
}

