package io.tpersson.ufw.adapters.durablemessages.kafka.outgoing

import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import org.apache.kafka.clients.producer.ProducerRecord

public class DefaultKafkaOutgoingMessageConverter(
    private val idHeaderKey: String = "id",
    private val typeHeaderKey: String = "type",
) : KafkaOutgoingMessageConverter {
    override fun convert(message: OutgoingMessage): ProducerRecord<ByteArray, ByteArray> {
        // TODO headers from metadata
        return ProducerRecord<ByteArray, ByteArray>(
            /* topic = */ message.topic,
            /* partition = */ null,
            /* timestamp = */ message.timestamp.toEpochMilli(),
            /* key = */ message.key?.toByteArray(Charsets.UTF_8),
            /* value = */ message.dataJson.toByteArray(Charsets.UTF_8)
        ).also {
            it.headers().add(idHeaderKey, message.id.value.toByteArray(Charsets.UTF_8))
            it.headers().add(typeHeaderKey, message.type.toByteArray(Charsets.UTF_8))
        }
    }
}