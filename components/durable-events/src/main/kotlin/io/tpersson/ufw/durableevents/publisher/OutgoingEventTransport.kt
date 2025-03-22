package io.tpersson.ufw.durableevents.publisher

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface OutgoingEventTransport {
    public suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork)
}