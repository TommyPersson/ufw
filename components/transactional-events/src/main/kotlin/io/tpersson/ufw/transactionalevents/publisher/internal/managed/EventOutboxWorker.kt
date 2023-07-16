package io.tpersson.ufw.transactionalevents.publisher.internal.managed

import io.tpersson.ufw.core.forever
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEvent
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.transactionalevents.publisher.internal.dao.EventOutboxDAO
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Duration
import java.util.*

@Singleton
public class EventOutboxWorker @Inject constructor(
    private val outboxNotifier: EventOutboxNotifier,
    private val outboxDAO: EventOutboxDAO,
    private val outgoingEventTransport: OutgoingEventTransport,
    private val unitOfWorkFactory: UnitOfWorkFactory,
) : ManagedJob() {

    private val logger = createLogger()

    private val pollInterval = Duration.ofSeconds(10)
    private val batchSize = 50

    override suspend fun launch() {
        forever(logger) {
            outboxNotifier.signal.wait(pollInterval)
            runOnce()
        }
    }

    private suspend fun runOnce() {
        // TODO grab table lock of some kind

        val batch = outboxDAO.getNextBatch(limit = batchSize)
        if (batch.isEmpty()) {
            return
        }

        val outgoingEvents = batch.map {
            OutgoingEvent(
                id = EventId(UUID.fromString(it.id)), // TODO use UUID in dao
                type = it.type,
                topic = it.topic,
                dataJson = it.dataJson,
                timestamp = it.timestamp
            )
        }

        unitOfWorkFactory.use { uow ->
            outgoingEventTransport.send(outgoingEvents, unitOfWork = uow)

            outboxDAO.deleteBatch(batch.map { it.uid }, uow)
        }
    }
}

