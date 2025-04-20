package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.adapters.durablemessages.kafka.utils.getHeaderValue
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.IncomingMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Instant

public class DefaultKafkaIncomingMessageConverter(
    private val idHeaderKey: String = "id",
    private val typeHeaderKey: String = "type",
) : KafkaIncomingMessageConverter {

    private val logger = createLogger()

    override fun convert(record: ConsumerRecord<ByteArray, ByteArray>): IncomingMessage? {
        val id = record.getHeaderValue(idHeaderKey)
        if (id == null) {
            logger.error("Cannot determine ID of message record, discarding it!")
            return null
        }

        val type = record.getHeaderValue(typeHeaderKey)
        if (type == null) {
            logger.error("Cannot determine type of message record, discarding it!")
            return null
        }

        val metadata = record.headers().filter {
            it.key().startsWith("meta.")
        }.associate {
            it.key().substringAfter("meta.") to it.value().toString(Charsets.UTF_8)
        } + mapOf(
            "kafkaKey" to record.key()?.toString(),
            "kafkaTopic" to record.topic(),
            "kafkaPartition" to record.partition().toString(),
            "kafkaOffset" to record.offset().toString(),
        )

        return IncomingMessage(
            id = DurableMessageId(id),
            type = type,
            dataJson = record.value().toString(Charsets.UTF_8),
            metadata = metadata,
            topic = record.topic(),
            timestamp = Instant.ofEpochMilli(record.timestamp())
        )
    }
}