package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import consumerRecordOf
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

internal class RecordBatchTest {

    @Test
    fun `commit - Commits the correct offsets`() {
        val mockConsumer = mock<Consumer<ByteArray, ByteArray>>()

        val batch = RecordBatch(
            records = listOf(
                consumerRecordOf("topic-2", partition = 2, offset = 6), // highest 2-2
                consumerRecordOf("topic-2", partition = 2, offset = 3),
                consumerRecordOf("topic-1", partition = 1, offset = 2),
                consumerRecordOf("topic-1", partition = 1, offset = 5), // highest 1-1
                consumerRecordOf("topic-1", partition = 2, offset = 6), // highest 1-2
                consumerRecordOf("topic-1", partition = 2, offset = 3),
                consumerRecordOf("topic-2", partition = 1, offset = 2),
                consumerRecordOf("topic-2", partition = 1, offset = 5), // highest 2-1
            ),
            consumer = mockConsumer,
        )

        batch.commit()

        verify(mockConsumer).commitSync(
            eq(
                mapOf(
                    TopicPartition("topic-1", 1) to OffsetAndMetadata(6),
                    TopicPartition("topic-1", 2) to OffsetAndMetadata(7),
                    TopicPartition("topic-2", 1) to OffsetAndMetadata(6),
                    TopicPartition("topic-2", 2) to OffsetAndMetadata(7),
                )
            ),
        )
    }
}