package io.tpersson.ufw.durableevents.publisher.transports

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durableevents.common.IncomingEvent
import io.tpersson.ufw.durableevents.common.IncomingEventIngester
import io.tpersson.ufw.durableevents.publisher.OutgoingEvent
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
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