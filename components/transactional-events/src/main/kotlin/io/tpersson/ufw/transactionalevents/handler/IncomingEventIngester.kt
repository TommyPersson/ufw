package io.tpersson.ufw.transactionalevents.handler

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface IncomingEventIngester {
    public suspend fun ingest(events: List<IncomingEvent>, unitOfWork: UnitOfWork)
}