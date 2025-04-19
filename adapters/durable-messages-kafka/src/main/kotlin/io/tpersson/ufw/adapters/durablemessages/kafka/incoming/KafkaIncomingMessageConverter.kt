package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.durablemessages.common.IncomingMessage
import org.apache.kafka.clients.consumer.ConsumerRecord

public interface KafkaIncomingMessageConverter {
    public fun convert(record: ConsumerRecord<ByteArray, ByteArray>): IncomingMessage?
}