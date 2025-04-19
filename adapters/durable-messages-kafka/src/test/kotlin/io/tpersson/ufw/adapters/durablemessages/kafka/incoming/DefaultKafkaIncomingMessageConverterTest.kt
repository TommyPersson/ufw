package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.tpersson.ufw.durablemessages.common.DurableMessageId
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DefaultKafkaIncomingMessageConverterTest {

    @Test
    fun `convert - Returns 'null' if the 'id' header is missing`() {
        val record = ConsumerRecord("test-topic", 1, 2, "key".toByteArray(), "value".toByteArray()).also {
            it.headers().add("type", "test-type".toByteArray())
        }

        val message = DefaultKafkaIncomingMessageConverter().convert(record)

        assertThat(message).isNull()
    }

    @Test
    fun `convert - Returns 'null' if the 'type' header is missing`() {
        val record = ConsumerRecord("test-topic", 1, 2, "key".toByteArray(), "value".toByteArray()).also {
            it.headers().add("id", "test-id".toByteArray())
        }

        val message = DefaultKafkaIncomingMessageConverter().convert(record)

        assertThat(message).isNull()
    }

    @Test
    fun `convert - Shall convert correctly`() {
        val record = ConsumerRecord("test-topic", 1, 2, "key".toByteArray(), "value".toByteArray()).also {
            it.headers().add("id", "test-id".toByteArray())
            it.headers().add("type", "test-type".toByteArray())
        }

        val message = DefaultKafkaIncomingMessageConverter().convert(record)!!

        assertThat(message.topic).isEqualTo("test-topic")
        assertThat(message.type).isEqualTo("test-type")
        assertThat(message.id).isEqualTo(DurableMessageId("test-id"))
        assertThat(message.dataJson).isEqualTo("value")
        assertThat(message.timestamp.toEpochMilli()).isEqualTo(-1) // Default for ConsumerRecord if not set
    }
}