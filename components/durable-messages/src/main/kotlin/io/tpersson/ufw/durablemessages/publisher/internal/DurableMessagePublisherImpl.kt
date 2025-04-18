package io.tpersson.ufw.durablemessages.publisher.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.publisher.DurableMessagePublisher
import io.tpersson.ufw.durablemessages.publisher.internal.dao.MessageEntityData
import io.tpersson.ufw.durablemessages.publisher.internal.dao.MessageOutboxDAO
import io.tpersson.ufw.durablemessages.publisher.internal.managed.MessageOutboxNotifier
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class DurableMessagePublisherImpl @Inject constructor(
    private val outboxDAO: MessageOutboxDAO,
    private val outboxNotifier: MessageOutboxNotifier,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
): DurableMessagePublisher {

    override fun publish(message: DurableMessage, unitOfWork: UnitOfWork) {
        publishAll(listOf(message), unitOfWork)
    }

    override fun publishAll(messages: List<DurableMessage>, unitOfWork: UnitOfWork) {
        val entities = messages.map { message ->
            val json = objectMapper.writeValueAsString(message)

            MessageEntityData(
                id = message.id.toString(),
                topic = message.topic,
                type = message.type,
                dataJson = json,
                ceDataJson = "{}",
                timestamp = message.timestamp
            )
        }

        outboxDAO.insertAll(entities, unitOfWork)

        unitOfWork.addPostCommitHook {
            outboxNotifier.signal.signal()
        }
    }
}
