package io.tpersson.ufw.adapters.durablemessages.kafka.outgoing

import org.apache.kafka.clients.producer.Producer

public fun interface KafkaProducerFactory {
    public fun create(config: Map<String, Any>): Producer<ByteArray, ByteArray>
}