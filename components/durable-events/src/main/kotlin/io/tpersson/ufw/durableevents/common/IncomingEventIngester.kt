package io.tpersson.ufw.durableevents.common

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface IncomingEventIngester {
    public suspend fun ingest(events: List<IncomingEvent>, unitOfWork: UnitOfWork)
}