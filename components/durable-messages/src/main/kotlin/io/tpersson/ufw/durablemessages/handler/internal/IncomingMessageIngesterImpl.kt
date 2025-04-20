package io.tpersson.ufw.durablemessages.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.WorkQueue
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId
import io.tpersson.ufw.durablemessages.common.IncomingMessage
import io.tpersson.ufw.durablemessages.common.IncomingMessageIngester
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Clock

@Singleton
public class IncomingMessageIngesterImpl @Inject constructor(
    private val messageHandlersProvider: DurableMessageHandlerRegistry,
    private val workQueue: WorkQueue,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
    private val clock: Clock,
) : IncomingMessageIngester {

    private val queuesByTopicAndType: Map<Pair<String, String>, List<DurableMessageQueueId>> by Memoized({ messageHandlersProvider.get() }) { handlers ->
        handlers.flatMap { it.findHandlerMethods() }.groupBy({ it.messageTopic to it.messageType }, { it.handler.queueId })
    }

    override suspend fun ingest(messages: List<IncomingMessage>, unitOfWork: UnitOfWork) {
        for (message in messages) {
            val queueIds = queuesByTopicAndType[message.topic to message.type] ?: emptyList()

            for (queueId in queueIds) {
                val workItem = createNewWorkItem(message, queueId, objectMapper)

                workQueue.schedule(workItem, clock.instant(), unitOfWork)
            }
        }
    }

    private fun createNewWorkItem(
        message: IncomingMessage,
        queueId: DurableMessageQueueId,
        objectMapper: ObjectMapper,
    ) = NewWorkItem(
        itemId = message.id.toWorkItemId(),
        queueId = queueId.toWorkItemQueueId(),
        type = message.type,
        dataJson = message.dataJson,
        metadataJson = objectMapper.writeValueAsString(message.metadata),
        scheduleFor = clock.instant(),
    )
}

