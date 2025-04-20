package io.tpersson.ufw.durablemessages.publisher.internal.managed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.configuration.DurableMessages
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import io.tpersson.ufw.durablemessages.publisher.internal.dao.MessageEntityData
import io.tpersson.ufw.durablemessages.publisher.internal.dao.MessageOutboxDAO
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Duration
import java.util.*

@Singleton
public class MessageOutboxWorker @Inject constructor(
    private val outboxNotifier: MessageOutboxNotifier,
    private val outboxDAO: MessageOutboxDAO,
    private val outgoingMessageTransport: OutgoingMessageTransport,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val databaseLocks: DatabaseLocks,
    private val configProvider: ConfigProvider,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : ManagedJob() {

    private val isEnabled = configProvider.get(Configs.DurableMessages.OutboxWorkerEnabled)
    private val pollInterval = configProvider.get(Configs.DurableMessages.OutboxWorkerInterval)
    private val batchSize = configProvider.get(Configs.DurableMessages.OutboxWorkerBatchSize)

    private val lock = databaseLocks.create("MessageOutboxWorker", UUID.randomUUID().toString())

    override suspend fun launch() {
        if (!isEnabled) {
            logger.warn("Outbox worker not enabled, exiting!")
            return
        }

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
                key = null, // TODO key
                type = it.type,
                topic = it.topic,
                dataJson = it.dataJson,
                metadata = objectMapper.readValue<Map<String, String>>(it.metadataJson),
                timestamp = it.timestamp
            )
        }

        unitOfWorkFactory.use { uow ->
            outgoingMessageTransport.send(outgoingMessages, unitOfWork = uow)

            outboxDAO.deleteBatch(batch.map { it.uid }, uow)
        }
    }
}

