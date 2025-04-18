package io.tpersson.ufw.durablemessages.publisher.internal.managed

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.managed.ManagedJob
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import io.tpersson.ufw.durablemessages.publisher.internal.dao.MessageEntityData
import io.tpersson.ufw.durablemessages.publisher.internal.dao.MessageOutboxDAO
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Duration
import java.util.*

@Singleton
public class MessageOutboxWorker @Inject constructor(
    private val outboxNotifier: MessageOutboxNotifier,
    private val outboxDAO: MessageOutboxDAO,
    private val outgoingMessageTransport: OutgoingMessageTransport,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val databaseLocks: DatabaseLocks
) : ManagedJob() {

    private val pollInterval = Duration.ofSeconds(10)
    private val batchSize = 50

    private val lock = databaseLocks.create("MessageOutboxWorker", UUID.randomUUID().toString())

    override suspend fun launch() {
        forever(logger) {
            outboxNotifier.signal.wait(pollInterval)
            runOnce()
        }
    }

    private suspend fun runOnce() {
        // TODO handle stale locks
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

    private suspend fun pollBatch(): List<MessageEntityData> {
        return outboxDAO.getNextBatch(limit = batchSize)
    }

    private suspend fun sendBatch(batch: List<MessageEntityData>) {
        val outgoingMessages = batch.map {
            OutgoingMessage(
                id = DurableMessageId(it.id),
                type = it.type,
                topic = it.topic,
                dataJson = it.dataJson,
                timestamp = it.timestamp
            )
        }

        unitOfWorkFactory.use { uow ->
            outgoingMessageTransport.send(outgoingMessages, unitOfWork = uow)

            outboxDAO.deleteBatch(batch.map { it.uid }, uow)
        }
    }
}

