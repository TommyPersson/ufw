package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.durablemessages.handler.internal.DurableMessageHandlerRegistry
import io.tpersson.ufw.managed.ManagedJob
import kotlinx.coroutines.coroutineScope
import java.time.Duration

public class KafkaIncomingMessageWorker(
    private val handlerRegistry: DurableMessageHandlerRegistry,
    private val batchProcessor: KafkaIncomingBatchProcessor,
    private val subscriber: KafkaConsumerSubscriber,
) : ManagedJob() {

    override suspend fun launch() {
        forever(logger, errorDelay = Duration.ofSeconds(5)) {
            coroutineScope {
                subscriber.start(handlerRegistry.topics, groupIdSuffix = "durable-messages-incoming-worker", this) {
                    batchProcessor.process(it)
                }
            }
        }
    }
}

