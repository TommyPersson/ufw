package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.databasequeue.*
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.toWorkItemQueueId
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessor.ProcessingResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
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
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

internal class DatabaseQueueWorkerTest {

    private lateinit var processorMock: SingleWorkItemProcessor
    private lateinit var processorFactoryMock: SingleWorkItemProcessorFactory
    private lateinit var workQueue: WorkQueue

    private lateinit var stateChanges: MutableSharedFlow<WorkItemStateChange>

    private lateinit var queueId: WorkItemQueueId

    private lateinit var worker: DatabaseQueueWorker

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        processorMock = mock()
        processorFactoryMock = mock()
        workQueue = mock()

        stateChanges = MutableSharedFlow(replay = 1)

        whenever(processorFactoryMock.create(any(), any())).thenReturn(processorMock)

        whenever(workQueue.stateChanges).thenReturn(stateChanges)

        queueId = "queue-1".toWorkItemQueueId()

        worker = DatabaseQueueWorker(
            queueId = queueId,
            handlersByType = emptyMap(),
            workQueue = workQueue,
            processorFactory = processorFactoryMock,
            adapterSettings = mock(),
            configProvider = ConfigProvider.empty(),
        )
    }

    @Test
    fun `Shall be able to cancel the worker even if there are more items available`(): Unit = runTest(timeout = 1.seconds) {
        val startedProcessingLatch = CompletableDeferred<Unit>()

        whenever(processorMock.processSingleItem(any(), any())).then {
            startedProcessingLatch.complete(Unit)
            ProcessingResult.PROCESSED
        }

        val coroutine = worker.start()

        stateChanges.emit(stubStateChange(toState = WorkItemState.SCHEDULED))

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
                queueId: WorkItemQueueId,
                typeHandlerMappings: Map<String, WorkItemHandler<*>>
            ): ProcessingResult {
                startedProcessingLatch.complete(Unit)
                cancellationLatch.await()
                wasActiveDeferred.complete(isActive)
                return ProcessingResult.PROCESSED
            }
        })

        worker = DatabaseQueueWorker(
            queueId = "queue-1".toWorkItemQueueId(),
            handlersByType = emptyMap(),
            workQueue = workQueue,
            processorFactory = processorFactoryMock,
            adapterSettings = mock(),
            configProvider = ConfigProvider.empty(),
        )

        val coroutine = worker.start()

        stateChanges.emit(stubStateChange(toState = WorkItemState.SCHEDULED))

        startedProcessingLatch.await()
        coroutine.cancel()
        cancellationLatch.complete(Unit)

        val wasActive = wasActiveDeferred.await()

        assertThat(wasActive).isTrue()
    }

    private fun stubStateChange(toState: WorkItemState) = WorkItemStateChange(
        queueId = queueId,
        itemId = WorkItemId("dont-care"),
        itemType = "dont-care",
        fromState = null,
        toState = toState,
        timestamp = Instant.now()
    )
}