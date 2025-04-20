package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import consumerRecordOf
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.durablemessages.common.IncomingMessageIngester
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.*

internal class KafkaIncomingBatchProcessorTest {

    private lateinit var messageIngester: IncomingMessageIngester
    private lateinit var incomingMessageConverter: KafkaIncomingMessageConverter
    private lateinit var unitOfWorkFactory: UnitOfWorkFactory
    private lateinit var unitOfWork: UnitOfWork

    private lateinit var batchProcessor: KafkaIncomingBatchProcessor

    @BeforeEach
    fun setUp() {
        messageIngester = mock()
        incomingMessageConverter = DefaultKafkaIncomingMessageConverter()

        unitOfWorkFactory = mock()
        unitOfWork = mock<UnitOfWork>()
        whenever(unitOfWorkFactory.create()).thenReturn(unitOfWork)

        batchProcessor = KafkaIncomingBatchProcessor(
            messageIngester = messageIngester,
            incomingMessageConverter = incomingMessageConverter,
            unitOfWorkFactory = unitOfWorkFactory
        )
    }

    @Test
    fun `process - Shall forward converted messages to ingester`(): Unit = runBlocking {
        val batch = RecordBatch(
            records = listOf(
                consumerRecordOf("topic-2", partition = 2, offset = 6),
                consumerRecordOf("topic-2", partition = 2, offset = 3),
                consumerRecordOf("topic-1", partition = 1, offset = 2),
            )
        )

        batchProcessor.process(batch)

        inOrder(messageIngester, unitOfWork) {
            verify(messageIngester).ingest(
                eq(
                    listOf(
                        incomingMessageConverter.convert(batch.records[0])!!,
                        incomingMessageConverter.convert(batch.records[1])!!,
                        incomingMessageConverter.convert(batch.records[2])!!,
                    )
                ),
                same(unitOfWork)
            )

            verify(unitOfWork).commit()
        }
    }

    @Test
    fun `process - Shall not ingest if no records are convertible`(): Unit = runBlocking {
        val batch = RecordBatch(
            records = emptyList()
        )

        batchProcessor.process(batch)

        inOrder(messageIngester, unitOfWork) {
            verify(messageIngester, times(0)).ingest(any(), any())
            verify(unitOfWork, times(0)).commit()
        }
    }

    @Test
    fun `process - Shall retry no more than 5 times`(): Unit = runBlocking {
        val batch = RecordBatch(
            records = listOf(
                consumerRecordOf("topic-2", partition = 2, offset = 6),
            )
        )

        whenever(messageIngester.ingest(any(), any())).thenThrow(RuntimeException("oh no"))

        assertThatThrownBy {
            runBlocking { batchProcessor.process(batch) }
        }.matches {
            it is RuntimeException && it.message == "oh no"
        }

        verify(messageIngester, times(5)).ingest(any(), any())
    }
}