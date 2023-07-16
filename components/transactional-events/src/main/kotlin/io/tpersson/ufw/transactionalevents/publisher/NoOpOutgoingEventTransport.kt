package io.tpersson.ufw.transactionalevents.publisher

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public class NoOpOutgoingEventTransport : OutgoingEventTransport {
    override suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork) {
    }
}