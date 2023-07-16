package io.tpersson.ufw.transactionalevents.publisher.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.publisher.TransactionalEventPublisher
import io.tpersson.ufw.transactionalevents.publisher.internal.dao.EventEntityData
import io.tpersson.ufw.transactionalevents.publisher.internal.dao.EventOutboxDAO
import io.tpersson.ufw.transactionalevents.publisher.internal.managed.EventOutboxNotifier
import io.tpersson.ufw.transactionalevents.type
import jakarta.inject.Inject
import jakarta.inject.Named

public class TransactionalEventPublisherImpl @Inject constructor(
    private val outboxDAO: EventOutboxDAO,
    private val outboxNotifier: EventOutboxNotifier,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
): TransactionalEventPublisher {

    override fun publish(topic: String, event: Event, unitOfWork: UnitOfWork) {
        publishAll(topic, listOf(event), unitOfWork)
    }

    override fun publishAll(topic: String, events: List<Event>, unitOfWork: UnitOfWork) {
        val entities = events.map { event ->
            val json = objectMapper.writeValueAsString(event)

            EventEntityData(
                id = event.id.toString(),
                topic = topic,
                type = event.type,
                dataJson = json,
                ceDataJson = "{}",
                timestamp = event.timestamp
            )
        }

        outboxDAO.insertAll(entities, unitOfWork)

        unitOfWork.addPostCommitHook {
            outboxNotifier.signal.signal()
        }
    }
}