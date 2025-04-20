package io.tpersson.ufw.adapters.durablemessages.kafka.outgoing

import io.tpersson.ufw.adapters.durablemessages.kafka.configuration.DurableMessagesKafka
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.Producer
import java.util.concurrent.Executors

@Singleton
public class KafkaOutgoingMessageTransport @Inject constructor(
    private val messageConverter: KafkaOutgoingMessageConverter = DefaultKafkaOutgoingMessageConverter(),
    private val configProvider: ConfigProvider,
    private val kafkaProducerFactory: KafkaProducerFactory,
) : OutgoingMessageTransport {

    private val kafkaProducer: Producer<ByteArray, ByteArray> by lazy {
        kafkaProducerFactory.create(configProvider.get(Configs.DurableMessagesKafka.Producer))
    }

    private val sendContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher() + NonCancellable

    override suspend fun send(
        messages: List<OutgoingMessage>,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.addPreCommitHook {
            val records = messages.map { messageConverter.convert(it) }

            withContext(sendContext) {
                for (record in records) {
                    kafkaProducer.send(record)
                }

                kafkaProducer.flush()
            }
        }
    }
}


