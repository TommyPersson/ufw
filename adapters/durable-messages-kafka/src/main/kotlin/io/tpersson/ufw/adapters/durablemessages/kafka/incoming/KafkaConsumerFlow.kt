package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.adapters.durablemessages.kafka.configuration.DurableMessagesKafka
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.logging.createLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.time.Duration

public class KafkaConsumerFlow(
    private val consumerFactory: KafkaConsumerFactory,
    private val configProvider: ConfigProvider,
) {
    private val logger = createLogger()

    public fun subscribe(topics: Set<String>): Flow<RecordBatch> {
        val consumer = consumerFactory.create(configProvider.get(Configs.DurableMessagesKafka.Consumer))

        return flow {
            while (true) {
                val records = withContext(Dispatchers.IO) {
                    consumer.poll(Duration.ofSeconds(5))?.toList() ?: emptyList()
                }

                if (records.isNotEmpty()) {
                    emit(RecordBatch(records, consumer))
                }

            }
        }.onStart {
            logger.info("Subscribing to topics: $topics")
            consumer.subscribe(topics)
        }.onCompletion {
            logger.info("Closing")
            consumer.close()
        }
    }
}