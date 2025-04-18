package io.tpersson.ufw.durablemessages.common

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface IncomingMessageIngester {
    public suspend fun ingest(messages: List<IncomingMessage>, unitOfWork: UnitOfWork)
}