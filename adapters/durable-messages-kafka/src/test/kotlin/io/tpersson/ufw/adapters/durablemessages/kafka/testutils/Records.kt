import org.apache.kafka.clients.consumer.ConsumerRecord
import java.util.*

public fun consumerRecordOf(
    topic: String,
    partition: Int = 0,
    offset: Int = 0,
    key: String = "",
    value: String = "",
): ConsumerRecord<ByteArray, ByteArray> {
    return ConsumerRecord(topic, partition, offset.toLong(), key.toByteArray(), value.toByteArray()).also {
        it.headers().add("id", UUID.randomUUID().toString().toByteArray())
        it.headers().add("type", "test-type".toByteArray())
    }
}