package io.tpersson.ufw.transactionalevents.publisher.transports

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEvent
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport
import jakarta.inject.Inject

public class NoOpOutgoingEventTransport @Inject constructor() : OutgoingEventTransport {
    override suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork) {
    }
}