package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.adapters.durablemessages.kafka.utils.getHeaderValue
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.IncomingMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Instant

public class DefaultKafkaIncomingMessageConverter : KafkaIncomingMessageConverter {

    private val logger = createLogger()

    override fun convert(record: ConsumerRecord<ByteArray, ByteArray>): IncomingMessage? {
        val id = record.getHeaderValue("id")
        if (id == null) {
            logger.error("Cannot determine ID of message record, discarding it!")
            return null
        }

        val type = record.getHeaderValue("type")
        if (type == null) {
            logger.error("Cannot determine type of message record, discarding it!")
            return null
        }

        return IncomingMessage(
            id = DurableMessageId(id),
            type = type,
            dataJson = record.value().toString(Charsets.UTF_8),
            topic = record.topic(),
            timestamp = Instant.ofEpochMilli(record.timestamp())
        )
    }
}