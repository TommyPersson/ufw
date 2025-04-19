package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

public class RecordBatch(
    public val records: List<ConsumerRecord<ByteArray, ByteArray>>,
    private val consumer: Consumer<ByteArray, ByteArray>
) {
    private val committableOffsets: Map<TopicPartition, OffsetAndMetadata> = records.groupBy {
        TopicPartition(it.topic(), it.partition())
    }.mapValues { OffsetAndMetadata(it.value.maxOf { record -> record.offset() } + 1) }

    public fun commit() {
        consumer.commitSync(committableOffsets)
    }
}