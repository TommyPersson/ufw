package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.adapters.durablemessages.kafka.configuration.DurableMessagesKafka
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.durablemessages.handler.internal.DurableMessageHandlerRegistry
import io.tpersson.ufw.managed.ManagedJob
import kotlinx.coroutines.coroutineScope
import java.time.Duration

public class KafkaIncomingMessageWorker(
    private val handlerRegistry: DurableMessageHandlerRegistry,
    private val batchProcessor: KafkaIncomingBatchProcessor,
    private val subscriber: KafkaConsumerSubscriber,
    private val configProvider: ConfigProvider,
) : ManagedJob() {


    override suspend fun launch() {
        val isEnabled = configProvider.get(Configs.DurableMessagesKafka.ConsumerEnabled)

        if (!isEnabled) {
            logger.warn("Kafka consumer not enabled, exiting.")
            return
        }

        forever(logger, errorDelay = Duration.ofSeconds(5)) {
            coroutineScope {
                subscriber.start(handlerRegistry.topics, groupIdSuffix = "durable-messages-incoming-worker", this) {
                    batchProcessor.process(it)
                }
            }
        }
    }
}

