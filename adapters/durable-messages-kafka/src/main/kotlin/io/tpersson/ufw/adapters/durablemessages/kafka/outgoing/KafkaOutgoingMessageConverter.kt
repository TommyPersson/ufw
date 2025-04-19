package io.tpersson.ufw.adapters.durablemessages.kafka.outgoing

import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import org.apache.kafka.clients.producer.ProducerRecord

public interface KafkaOutgoingMessageConverter {
    public fun convert(message: OutgoingMessage): ProducerRecord<ByteArray, ByteArray>
}