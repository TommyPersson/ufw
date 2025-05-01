package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import consumerRecordOf
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.configuration.ConfigProvider
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.consumer.internals.AutoOffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

internal class KafkaConsumerSubscriberTest {

    private lateinit var mockConsumer: MockConsumer<ByteArray, ByteArray>
    private lateinit var consumerFactory: KafkaConsumerFactory

    private lateinit var subscriber: KafkaConsumerSubscriber

    @BeforeEach
    fun setUp() {
        mockConsumer = MockConsumer<ByteArray, ByteArray>(AutoOffsetResetStrategy.EARLIEST.name())

        consumerFactory = object : KafkaConsumerFactory {
            override fun create(config: Map<String, Any>): Consumer<ByteArray, ByteArray> {
                return mockConsumer
            }
        }

        subscriber = KafkaConsumerSubscriber(
            consumerFactory = consumerFactory,
            configProvider = ConfigProvider.empty(),
            appInfoProvider = AppInfoProvider.simple(name = "my-app"),
        )
    }

    @Test
    fun `start - Processes records until cancelled`(): Unit = runBlocking {
        val received = mutableListOf<RecordBatch>()

        mockConsumer.updateBeginningOffsets(
            mapOf(
                TopicPartition("topic-1", 0) to 0L,
                TopicPartition("topic-2", 0) to 0L,
            )
        )

        val subscriberJob = subscriber.start(setOf("topic-1", "topic-2"), "the-suffix", this) {
            received.add(it)
        }

        await.until {
            mockConsumer.subscription() == setOf("topic-1", "topic-2")
        }

        mockConsumer.schedulePollTask {
            mockConsumer.rebalance(
                listOf(
                    TopicPartition("topic-1", 0),
                    TopicPartition("topic-2", 0),
                )
            )

            mockConsumer.addRecord(consumerRecordOf("topic-1", partition = 0, offset = 1))
            mockConsumer.addRecord(consumerRecordOf("topic-1", partition = 0, offset = 2))
            mockConsumer.addRecord(consumerRecordOf("topic-2", partition = 0, offset = 1))
        }

        mockConsumer.schedulePollTask {
            mockConsumer.addRecord(consumerRecordOf("topic-2", partition = 0, offset = 2))
            mockConsumer.addRecord(consumerRecordOf("topic-1", partition = 0, offset = 3))
        }

        await.until {
            received.size == 2
        }

        val topic1Offsets = received.flatMap { batch ->
            batch.records.filter { it.topic() == "topic-1" }.map { it.offset() }
        }
        assertThat(topic1Offsets).containsSequence(1, 2, 3)

        val topic2Offsets = received.flatMap { batch ->
            batch.records.filter { it.topic() == "topic-1" }.map { it.offset() }
        }
        assertThat(topic2Offsets).containsSequence(1, 2)

        assertThat(
            mockConsumer.committed(
                setOf(
                    TopicPartition("topic-1", 0),
                    TopicPartition("topic-2", 0)
                )
            )
        ).isEqualTo(
            mapOf(
                TopicPartition("topic-1", 0) to OffsetAndMetadata(4),
                TopicPartition("topic-2", 0) to OffsetAndMetadata(3),
            )
        )

        subscriberJob.cancel()

        await.until {
            mockConsumer.closed()
        }
    }

    @Test
    fun `start - Sets up the group ID correctly`(): Unit = runBlocking {
        val factoryMock = mock<KafkaConsumerFactory>()
        whenever(factoryMock.create(any())).thenReturn(mock())

        val subscriber2 = KafkaConsumerSubscriber(
            consumerFactory = factoryMock,
            configProvider = ConfigProvider.empty(),
            appInfoProvider = AppInfoProvider.simple(name = "my-app"),
        )

        val job = subscriber2.start(setOf("topic-3", "topic-4"), "the-suffix", this) {
            // no-op
        }

        delay(10)

        job.cancelAndJoin()

        verify(factoryMock).create(argWhere { it[ConsumerConfig.GROUP_ID_CONFIG] == "my-app--the-suffix" })
    }
}