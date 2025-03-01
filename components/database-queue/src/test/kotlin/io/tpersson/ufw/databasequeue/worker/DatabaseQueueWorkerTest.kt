package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.WorkItemHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

internal class DatabaseQueueWorkerTest {

    private lateinit var processorMock: SingleWorkItemProcessor
    private lateinit var processorFactoryMock: SingleWorkItemProcessorFactory

    private lateinit var worker: DatabaseQueueWorker

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        processorMock = mock()
        processorFactoryMock = mock()

        whenever(processorFactoryMock.create(any(), any())).thenReturn(processorMock)

        worker = DatabaseQueueWorker(
            queueId = "queue-1",
            handlersByType = emptyMap(),
            processorFactory = processorFactoryMock,
            mdcLabels = mock()
        )
    }

    @Test
    fun `Shall be able to cancel the worker even if there are more items available`(): Unit = runTest(timeout = 1.seconds) {
        val startedProcessingLatch = CompletableDeferred<Unit>()

        whenever(processorMock.processSingleItem(any(), any())).then {
            startedProcessingLatch.complete(Unit)
            true
        }

        val coroutine = worker.start()

        startedProcessingLatch.await()

        coroutine.cancelAndJoin()
    }

    @Test
    fun `Shall not allow cancellation during processing`(): Unit = runTest(timeout = 1.seconds) {
        reset(processorFactoryMock)

        val startedProcessingLatch = CompletableDeferred<Unit>()
        val cancellationLatch = CompletableDeferred<Unit>()

        val wasActiveDeferred = CompletableDeferred<Boolean>()

        whenever(processorFactoryMock.create(any(), any())).thenReturn(object : SingleWorkItemProcessor {
            override suspend fun processSingleItem(
                queueId: String,
                typeHandlerMappings: Map<String, WorkItemHandler<*>>
            ): Boolean {
                startedProcessingLatch.complete(Unit)
                cancellationLatch.await()
                wasActiveDeferred.complete(isActive)
                return true
            }
        })

        worker = DatabaseQueueWorker(
            queueId = "queue-1",
            handlersByType = emptyMap(),
            processorFactory = processorFactoryMock,
            mdcLabels = mock()
        )

        val coroutine = worker.start()

        startedProcessingLatch.await()
        coroutine.cancel()
        cancellationLatch.complete(Unit)

        val wasActive = wasActiveDeferred.await()

        assertThat(wasActive).isTrue()
    }
}