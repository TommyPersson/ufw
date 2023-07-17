package io.tpersson.ufw.transactionalevents.publisher.transports

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent
import io.tpersson.ufw.transactionalevents.handler.IncomingEventIngester
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEvent
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport
import jakarta.inject.Inject

public class DirectOutgoingEventTransport @Inject constructor(
    private val ingester: IncomingEventIngester
) : OutgoingEventTransport {
    override suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork) {
        val incomingEvents = events.map { it.asIncomingEvent() }
        ingester.ingest(incomingEvents, unitOfWork)
    }
}

private fun OutgoingEvent.asIncomingEvent() = IncomingEvent(
    id = id,
    type = type,
    topic = topic,
    dataJson = dataJson,
    timestamp = timestamp,
)