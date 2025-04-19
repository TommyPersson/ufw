package io.tpersson.ufw.adapters.durablemessages.kafka.outgoing

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer

public class KafkaProducerFactoryImpl : KafkaProducerFactory {

    private val producerConfigDefaults = mapOf(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
    )

    override fun create(config: Map<String, Any>): KafkaProducer<ByteArray, ByteArray> {
        val producerConfig = producerConfigDefaults + config
        return KafkaProducer(producerConfig)
    }
}