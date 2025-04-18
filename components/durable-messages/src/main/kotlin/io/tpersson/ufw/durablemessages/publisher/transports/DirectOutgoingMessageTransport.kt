package io.tpersson.ufw.durablemessages.publisher.transports

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablemessages.common.IncomingMessage
import io.tpersson.ufw.durablemessages.common.IncomingMessageIngester
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class DirectOutgoingMessageTransport @Inject constructor(
    private val ingester: IncomingMessageIngester
) : OutgoingMessageTransport {

    override suspend fun send(messages: List<OutgoingMessage>, unitOfWork: UnitOfWork) {
        val incomingMessages = messages.map { it.asIncomingMessage() }
        ingester.ingest(incomingMessages, unitOfWork)
    }
}

private fun OutgoingMessage.asIncomingMessage() = IncomingMessage(
    id = id,
    type = type,
    topic = topic,
    dataJson = dataJson,
    timestamp = timestamp,
)