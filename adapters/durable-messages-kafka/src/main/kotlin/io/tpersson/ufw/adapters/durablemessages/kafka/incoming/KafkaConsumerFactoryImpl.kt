package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer

public class KafkaConsumerFactoryImpl : KafkaConsumerFactory {

    private val consumerConfigDefaults = mapOf(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
    )

    override fun create(config: Map<String, Any>): Consumer<ByteArray, ByteArray> {
        val consumerConfig = consumerConfigDefaults + config
        return KafkaConsumer(consumerConfig)
    }
}