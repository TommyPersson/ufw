package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.adapters.durablemessages.kafka.configuration.DurableMessagesKafka
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.logging.createLogger
import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import java.util.concurrent.Executors

public class KafkaConsumerSubscriber(
    private val consumerFactory: KafkaConsumerFactory,
    private val configProvider: ConfigProvider,
    private val appInfoProvider: AppInfoProvider,
) {
    private val logger = createLogger()

    private val pollWaitTime = configProvider.get(Configs.DurableMessagesKafka.ConsumerPollWaitTime)

    private val consumerContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    public fun start(
        topics: Set<String>,
        groupIdSuffix: String,
        scope: CoroutineScope,
        handler: suspend (batch: RecordBatch) -> Unit
    ): Job {
        return scope.launch(consumerContext) {
            val consumerConfig = createConfiguration(groupIdSuffix)
            val consumer = consumerFactory.create(consumerConfig)

            logger.info("Subscribing to topics: $topics")
            consumer.subscribe(topics, object : ConsumerRebalanceListener {
                override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>) {
                    consumer.commitSync()
                }

                override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>) {
                }
            })

            try {
                while (isActive) {
                    withContext(NonCancellable) {
                        val records = consumer.poll(pollWaitTime)?.toList() ?: emptyList()
                        if (records.isNotEmpty()) {
                            handler(RecordBatch(records))
                            consumer.commitSync()
                        }
                    }
                }
            } finally {
                logger.info("Closing")
                consumer.close()
            }
        }
    }

    private fun createConfiguration(groupIdSuffix: String): Map<String, Any> {
        val groupId = appInfoProvider.get().name + "--" + groupIdSuffix
        val baseConfig = configProvider.get(Configs.DurableMessagesKafka.Consumer)
        return baseConfig + mapOf(ConsumerConfig.GROUP_ID_CONFIG to groupId)
    }
}