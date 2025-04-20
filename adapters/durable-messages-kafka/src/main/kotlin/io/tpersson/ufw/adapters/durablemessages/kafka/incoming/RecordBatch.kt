package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import org.apache.kafka.clients.consumer.ConsumerRecord

public class RecordBatch(
    public val records: List<ConsumerRecord<ByteArray, ByteArray>>,
)