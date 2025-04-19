package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId
import io.tpersson.ufw.durablemessages.common.IncomingMessage
import io.tpersson.ufw.durablemessages.common.IncomingMessageIngester
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock

@Singleton
public class IncomingMessageIngesterImpl @Inject constructor(
    private val messageHandlersProvider: DurableMessageHandlerRegistry,
    private val workItemsDAO: WorkItemsDAO,
    private val clock: Clock,
) : IncomingMessageIngester {

    private val queuesByTopicAndType: Map<Pair<String, String>, List<DurableMessageQueueId>> by Memoized({ messageHandlersProvider.get() }) { handlers ->
        handlers.flatMap { it.findHandlerMethods() }.groupBy({ it.messageTopic to it.messageType }, { it.handler.queueId })
    }

    override suspend fun ingest(messages: List<IncomingMessage>, unitOfWork: UnitOfWork) {
        for (message in messages) {
            val queueIds = queuesByTopicAndType[message.topic to message.type] ?: emptyList()

            for (queueId in queueIds) {
                val workItem = createNewWorkItem(message, queueId)

                workItemsDAO.scheduleNewItem(workItem, clock.instant(), unitOfWork)
            }
        }
    }

    private fun createNewWorkItem(
        message: IncomingMessage,
        queueId: DurableMessageQueueId
    ) = NewWorkItem(
        itemId = message.id.toWorkItemId(),
        queueId = queueId.toWorkItemQueueId(),
        type = message.type,
        metadataJson = "{}", // TODOm
        dataJson = message.dataJson,
        scheduleFor = clock.instant(),
    )
}

