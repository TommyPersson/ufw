package io.tpersson.ufw.durablemessages.publisher

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface OutgoingMessageTransport {
    public suspend fun send(messages: List<OutgoingMessage>, unitOfWork: UnitOfWork)
}