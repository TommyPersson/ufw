package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import org.apache.kafka.clients.consumer.Consumer

public interface KafkaConsumerFactory {
    public fun create(config: Map<String, Any>): Consumer<ByteArray, ByteArray>
}

