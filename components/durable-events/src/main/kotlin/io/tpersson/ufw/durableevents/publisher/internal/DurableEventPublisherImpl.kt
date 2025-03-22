package io.tpersson.ufw.durableevents.publisher.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durableevents.common.DurableEvent
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import io.tpersson.ufw.durableevents.publisher.internal.dao.EventEntityData
import io.tpersson.ufw.durableevents.publisher.internal.dao.EventOutboxDAO
import io.tpersson.ufw.durableevents.publisher.internal.managed.EventOutboxNotifier
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class DurableEventPublisherImpl @Inject constructor(
    private val outboxDAO: EventOutboxDAO,
    private val outboxNotifier: EventOutboxNotifier,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
): DurableEventPublisher {

    override fun publish(event: DurableEvent, unitOfWork: UnitOfWork) {
        publishAll(listOf(event), unitOfWork)
    }

    override fun publishAll(events: List<DurableEvent>, unitOfWork: UnitOfWork) {
        val entities = events.map { event ->
            val json = objectMapper.writeValueAsString(event)

            EventEntityData(
                id = event.id.toString(),
                topic = event.topic,
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
