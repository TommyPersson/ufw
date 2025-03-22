package io.tpersson.ufw.durableevents.publisher.transports

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durableevents.publisher.OutgoingEvent
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import jakarta.inject.Inject

public class NoOpOutgoingEventTransport @Inject constructor() : OutgoingEventTransport {
    override suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork) {
    }
}