package io.tpersson.ufw.durableevents.publisher.internal.managed

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durableevents.common.DurableEventId
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.durableevents.publisher.OutgoingEvent
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.durableevents.publisher.internal.dao.EventEntityData
import io.tpersson.ufw.durableevents.publisher.internal.dao.EventOutboxDAO
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
    private val databaseLocks: DatabaseLocks
) : ManagedJob() {

    private val pollInterval = Duration.ofSeconds(10)
    private val batchSize = 50

    private val lock = databaseLocks.create("EventOutboxWorker", UUID.randomUUID().toString())

    override suspend fun launch() {
        forever(logger) {
            outboxNotifier.signal.wait(pollInterval)
            runOnce()
        }
    }

    private suspend fun runOnce() {
        val lockHandle = lock.tryAcquire() ?: return

        try {
            while (true) {
                val batch = pollBatch()
                if (batch.isEmpty()) {
                    return
                }

                sendBatch(batch)
            }
        } finally {
            lockHandle.release()
        }
    }

    private suspend fun pollBatch(): List<EventEntityData> {
        return outboxDAO.getNextBatch(limit = batchSize)
    }

    private suspend fun sendBatch(batch: List<EventEntityData>) {
        val outgoingEvents = batch.map {
            OutgoingEvent(
                id = DurableEventId(it.id),
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

