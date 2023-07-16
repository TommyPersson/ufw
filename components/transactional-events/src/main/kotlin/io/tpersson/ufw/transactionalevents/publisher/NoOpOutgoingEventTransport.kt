package io.tpersson.ufw.transactionalevents.publisher

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject

public class NoOpOutgoingEventTransport @Inject constructor() : OutgoingEventTransport {
    override suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork) {
    }
}