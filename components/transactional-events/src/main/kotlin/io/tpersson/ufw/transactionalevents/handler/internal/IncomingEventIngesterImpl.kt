package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.handler.EventState
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent
import io.tpersson.ufw.transactionalevents.handler.IncomingEventIngester
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAO
import jakarta.inject.Inject
import java.time.InstantSource

public class IncomingEventIngesterImpl @Inject constructor(
    private val eventHandlersProvider: EventHandlersProvider,
    private val eventQueueDAO: EventQueueDAO,
    private val clock: InstantSource,
) : IncomingEventIngester {
    override fun ingest(events: List<IncomingEvent>, unitOfWork: UnitOfWork) {
        val now = clock.instant()

        for (event in events) {
            val eventEntity = EventEntityData(
                queueId = "the-test-queue",
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

            eventQueueDAO.insert(eventEntity, unitOfWork = unitOfWork)
        }
    }
}


