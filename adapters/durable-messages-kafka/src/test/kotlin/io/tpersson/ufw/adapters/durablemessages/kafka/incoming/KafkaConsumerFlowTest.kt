package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import consumerRecordOf
import io.tpersson.ufw.core.configuration.ConfigProvider
import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.verify

internal class KafkaConsumerFlowTest {

    private lateinit var mockConsumer: MockConsumer<ByteArray, ByteArray>
    private lateinit var consumerFactory: KafkaConsumerFactory

    private lateinit var consumerFlow: KafkaConsumerFlow

    @BeforeEach
    fun setUp() {
        mockConsumer = MockConsumer<ByteArray, ByteArray>(OffsetResetStrategy.EARLIEST)

        consumerFactory = object : KafkaConsumerFactory {
            override fun create(config: Map<String, Any>): Consumer<ByteArray, ByteArray> {
                return mockConsumer
            }
        }

        consumerFlow = KafkaConsumerFlow(
            consumerFactory = consumerFactory,
            configProvider = ConfigProvider.empty()
        )
    }

    @Test
    fun `subscribe - Returns flow emitting published records`(): Unit = runBlocking {
        val received = mutableListOf<RecordBatch>()

        mockConsumer.updateBeginningOffsets(
            mapOf(
                TopicPartition("topic-1", 0) to 0L,
                TopicPartition("topic-2", 0) to 0L,
            )
        )

        val collectorJob = launch(Dispatchers.Default) {
            consumerFlow.subscribe(setOf("topic-1", "topic-2")).collect {
                received.add(it)
            }
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

        collectorJob.cancel()

        await.until {
            mockConsumer.closed()
        }
    }
}