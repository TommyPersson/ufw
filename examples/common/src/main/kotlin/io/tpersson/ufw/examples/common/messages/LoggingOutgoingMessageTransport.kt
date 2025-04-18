package io.tpersson.ufw.examples.common.messages

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport

public class LoggingOutgoingMessageTransport : OutgoingMessageTransport {

    private val logger = createLogger()

    override suspend fun send(messages: List<OutgoingMessage>, unitOfWork: UnitOfWork) {
        logger.info("Pretending to send $messages")
    }
}