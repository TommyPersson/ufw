package io.tpersson.ufw.adapters.durablemessages.kafka.outgoing

import io.tpersson.ufw.adapters.durablemessages.kafka.utils.getHeaderValue
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.database.testing.FakeUnitOfWork
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.RoundRobinPartitioner
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

internal class KafkaOutgoingMessageTransportTest {

    private lateinit var mockProducer: MockProducer<ByteArray, ByteArray>

    private lateinit var transport: KafkaOutgoingMessageTransport

    @BeforeEach
    fun setUp() {
        mockProducer = MockProducer(
            true,
            RoundRobinPartitioner(),
            ByteArraySerializer(),
            ByteArraySerializer()
        )

        transport = KafkaOutgoingMessageTransport(
            messageConverter = DefaultKafkaOutgoingMessageConverter(),
            configProvider = ConfigProvider.empty(),
            kafkaProducerFactory = { mockProducer }
        )
    }

    @Test
    fun `send - Forwards to Kafka Producer `(): Unit = runBlocking {
        val unitOfWork = FakeUnitOfWork()

        transport.send(
            listOf(
                createMessage("1", topic = "topic-1"),
                createMessage("2", topic = "topic-2"),
                createMessage("3", topic = "topic-1"),
                createMessage("4", topic = "topic-4"),
            ),
            unitOfWork
        )

        unitOfWork.commit()

        assertThat(mockProducer.history()).hasSize(4)

        assertThat(mockProducer.history()[0].topic()).isEqualTo("topic-1")
        assertThat(mockProducer.history()[0].value()).isEqualTo("{}".toByteArray())
        assertThat(mockProducer.history()[0].key()).isEqualTo("key".toByteArray())
        assertThat(mockProducer.history()[0].timestamp()).isEqualTo(123L)
        assertThat(mockProducer.history()[0].getHeaderValue("id")).isEqualTo("1")
        assertThat(mockProducer.history()[0].getHeaderValue("type")).isEqualTo("test-type")

        assertThat(mockProducer.history()[1].topic()).isEqualTo("topic-2")
        assertThat(mockProducer.history()[2].topic()).isEqualTo("topic-1")
        assertThat(mockProducer.history()[3].topic()).isEqualTo("topic-4")

        assertThat(mockProducer.flushed()).isTrue()
    }

    private fun createMessage(id: String, topic: String) = OutgoingMessage(
        id = DurableMessageId(id),
        key = "key",
        type = "test-type",
        topic = topic,
        dataJson = "{}",
        metadata = mapOf(),
        timestamp = Instant.ofEpochMilli(123),
    )
}