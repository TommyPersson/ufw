package io.tpersson.ufw.examples.common.events

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEvent
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport

public class LoggingOutgoingEventTransport : OutgoingEventTransport {

    private val logger = createLogger()

    override suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork) {
        logger.info("Pretending to send $events")
    }
}