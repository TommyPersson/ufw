package io.tpersson.ufw.durablemessages.publisher.transports

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import jakarta.inject.Inject

public class NoOpOutgoingMessageTransport @Inject constructor() : OutgoingMessageTransport {
    override suspend fun send(messages: List<OutgoingMessage>, unitOfWork: UnitOfWork) {
    }
}