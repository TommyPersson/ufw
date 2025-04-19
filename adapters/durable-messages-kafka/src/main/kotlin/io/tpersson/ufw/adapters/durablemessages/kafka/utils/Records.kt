package io.tpersson.ufw.adapters.durablemessages.kafka.utils

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

public fun ConsumerRecord<ByteArray, ByteArray>.getHeaderValue(header: String): String? {
    return this.headers().lastHeader(header)?.value()?.toString(Charsets.UTF_8)
}

public fun ProducerRecord<ByteArray, ByteArray>.getHeaderValue(header: String): String? {
    return this.headers().lastHeader(header)?.value()?.toString(Charsets.UTF_8)
}
