package io.tpersson.ufw.databasequeue.worker

import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.utils.LoggerCache
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.*
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.internal.WorkQueueInternal
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessor.ProcessingResult
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.slf4j.Logger
import org.slf4j.MDC
import java.time.Instant
import java.time.Clock
import java.util.*

internal class SingleWorkItemProcessorImplTest {

    private lateinit var unitOfWorkFactory: UnitOfWorkFactory
    private lateinit var workQueue: WorkQueueInternal
    private lateinit var workItemFailuresDAO: WorkItemFailuresDAO
    private lateinit var workItemsDAO: WorkItemsDAO
    private lateinit var queueStateChecker: QueueStateChecker
    private lateinit var watchdogId: String

    private lateinit var clock: Clock
    private lateinit var now: Instant

    private lateinit var processor: SingleWorkItemProcessorImpl

    private val config = DatabaseQueueConfig()

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        watchdogId = UUID.randomUUID().toString()
        workQueue = mock()
        workItemsDAO = mock()
        workItemFailuresDAO = mock()
        queueStateChecker = mock()

        whenever(queueStateChecker.isQueuePaused(any())).thenReturn(false)

        unitOfWorkFactory = mock()
        whenever(unitOfWorkFactory.create()).then { mock<UnitOfWork>() }

        now = Instant.now()

        clock = mock()
        whenever(clock.instant()).then { now }

        TestWorkItem1Handler.reset()

        processor = SingleWorkItemProcessorImpl(
            watchdogId = watchdogId,
            workQueue = workQueue,
            workItemsDAO = workItemsDAO,
            workItemFailuresDAO = workItemFailuresDAO,
            queueStateChecker = queueStateChecker,
            unitOfWorkFactory = unitOfWorkFactory,
            meterRegistry = SimpleMeterRegistry(),
            clock = clock,
            adapterSettings = TestAdapterSettings,
            config = config,
        )
    }

    @Test
    fun `processSingleItem - Returns 'SKIPPED_NO_ITEM_AVAILABLE' if no item was taken from the queue`(): Unit =
        runBlocking {
            val queueId = "queue-1".toWorkItemQueueId()

            stubNextWorkItem(
                item = null,
                queueId = queueId.value
            )

            val result = processor.processSingleItem(queueId, emptyMap())

            assertThat(result).isEqualTo(ProcessingResult.SKIPPED_NO_ITEM_AVAILABLE)

            verify(workQueue).takeNext(eq(queueId), eq(watchdogId), eq(now))
        }

    @Test
    fun `processSingleItem - Returns 'SKIPPED_QUEUE_PAUSED' if the queue is paused`(): Unit = runBlocking {
        val queueId = "queue-1".toWorkItemQueueId()

        stubNextWorkItem(
            item = null,
            queueId = queueId.value
        )

        whenever(queueStateChecker.isQueuePaused(eq(queueId))).thenReturn(true)

        val result = processor.processSingleItem(queueId, emptyMap())

        assertThat(result).isEqualTo(ProcessingResult.SKIPPED_QUEUE_PAUSED)

        verify(workItemsDAO, never()).markInProgressItemAsSuccessful(any(), any(), any(), any(), any(), any())
        verify(workItemsDAO, never()).markInProgressItemAsFailed(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `processSingleItem - Returns 'PROCESSED' if an item was taken from the queue but no mapping was found`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = UnmappedTestWorkItem(),
                queueId = "queue-1"
            )!!

            val result = processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

            assertThat(result).isEqualTo(ProcessingResult.PROCESSED)

            verify(workItemsDAO, never()).markInProgressItemAsSuccessful(any(), any(), any(), any(), any(), any())
            verify(workItemsDAO, never()).markInProgressItemAsFailed(any(), any(), any(), any(), any(), any())
        }

    @Test
    fun `processSingleItem - Returns 'PROCESSED' if an item was taken from the queue and successfully processed`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = false),
                queueId = "queue-1"
            )!!

            val result = processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

            assertThat(result).isEqualTo(ProcessingResult.PROCESSED)
        }

    @Test
    fun `processSingleItem - Returns 'PROCESSED' if an item was taken from the queue and failing processed`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = true),
                queueId = "queue-1"
            )!!

            val result = processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

            assertThat(result).isEqualTo(ProcessingResult.PROCESSED)
        }

    @Test
    fun `processSingleItem - Marks item as successful on successful processing`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        verify(workQueue).markInProgressItemAsSuccessful(
            item = eq(stubbedWorkItem),
            expiresAt = eq(now.plus(config.successfulItemExpirationDelay)),
            watchdogId = eq(watchdogId),
            now = eq(now),
            unitOfWork = same(TestWorkItem1Handler.successContextUnitOfWork!!)
        )

        verify(TestWorkItem1Handler.successContextUnitOfWork!!).commit()
    }

    @Test
    fun `processSingleItem - Marks item as failed on failed processing`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = true),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        verify(workQueue).markInProgressItemAsFailed(
            item = eq(stubbedWorkItem),
            expiresAt = eq(now.plus(config.failedItemExpirationDelay)),
            watchdogId = eq(watchdogId),
            now = eq(now),
            unitOfWork = same(TestWorkItem1Handler.failureContextUnitOfWork!!)
        )

        verify(TestWorkItem1Handler.failureContextUnitOfWork!!).commit()
    }

    @Test
    fun `processSingleItem - Shall reschedule failed items according to FailureAction_RescheduleNow`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = true),
                queueId = "queue-1"
            )!!

            TestWorkItem1Handler.failureAction = FailureAction.RescheduleNow

            processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

            verify(workQueue).rescheduleInProgressItem(
                item = eq(stubbedWorkItem),
                scheduleFor = same(now),
                watchdogId = eq(watchdogId),
                now = eq(now),
                unitOfWork = same(TestWorkItem1Handler.failureContextUnitOfWork!!),
            )
        }

    @Test
    fun `processSingleItem - Shall reschedule failed items according to FailureAction_RescheduleAt`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = true),
                queueId = "queue-1"
            )!!

            val rescheduleAt = Instant.now()

            TestWorkItem1Handler.failureAction = FailureAction.RescheduleAt(rescheduleAt)

            processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

            verify(workQueue).rescheduleInProgressItem(
                item = eq(stubbedWorkItem),
                scheduleFor = same(rescheduleAt),
                watchdogId = eq(watchdogId),
                now = eq(now),
                unitOfWork = same(TestWorkItem1Handler.failureContextUnitOfWork!!),
            )
        }

    @Test
    fun `processSingleItem - Shall store a failure entry whenever an item fails processing`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = true),
            queueId = "queue-1"
        )!!

        val rescheduleAt = Instant.now()

        TestWorkItem1Handler.failureAction = FailureAction.RescheduleAt(rescheduleAt)

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        verify(workItemFailuresDAO).insertFailure(
            failure = argWhere {
                it.id.isNotEmpty() &&
                        it.itemUid == stubbedWorkItem.uid &&
                        it.timestamp == now &&
                        it.errorType == "IllegalStateException" &&
                        it.errorMessage == "fail" &&
                        it.errorStackTrace.contains("TestWorkItem1Handler")
            },
            unitOfWork = same(TestWorkItem1Handler.failureContextUnitOfWork!!),
        )
    }

    @Test
    fun `processSingleItem - Shall provide a data to the failure handler`(): Unit = runBlocking {
        val workItem = TestWorkItem1(shouldFail = true)

        val stubbedWorkItem = stubNextWorkItem(
            item = workItem,
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        assertThat(TestWorkItem1Handler.failureItem).isEqualTo(workItem)

        assertThat(TestWorkItem1Handler.failureError?.message).isEqualTo("fail")

        assertThat(TestWorkItem1Handler.failureContext?.failureCount).isEqualTo(1)
        assertThat(TestWorkItem1Handler.failureContext?.clock).isSameAs(clock)
        assertThat(TestWorkItem1Handler.failureContext?.timestamp).isSameAs(now)
    }

    @Test
    fun `processSingleItem - Shall store a failure when item transformation fails`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = UnparsableWorkItem(),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        val unitOfWorkCaptor = argumentCaptor<UnitOfWork>()

        verify(workQueue).markInProgressItemAsFailed(
            item = eq(stubbedWorkItem),
            expiresAt = eq(now.plus(config.failedItemExpirationDelay)),
            watchdogId = eq(watchdogId),
            now = eq(now),
            unitOfWork = unitOfWorkCaptor.capture()
        )

        verify(workItemFailuresDAO).insertFailure(
            failure = argWhere {
                it.id.isNotEmpty() &&
                        it.itemUid == stubbedWorkItem.uid &&
                        it.timestamp == now &&
                        it.errorType == "UnableToTransformWorkItemException" &&
                        it.errorMessage.isNotEmpty() &&
                        it.errorStackTrace.contains("unable to parse item") // The inner error
            },
            unitOfWork = unitOfWorkCaptor.capture(),
        )

        assertThat(unitOfWorkCaptor.allValues.distinct()).hasSize(1)

        verify(unitOfWorkCaptor.firstValue).commit()
    }

    @Test
    fun `processSingleItem - Shall not commit UnitOfWork in handling context on failure`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = true),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        verify(TestWorkItem1Handler.successContextUnitOfWork!!, never()).commit()
        verify(TestWorkItem1Handler.failureContextUnitOfWork!!).commit()
    }

    @Test
    fun `processSingleItem - Shall not commit UnitOfWork in failure context on success`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = false),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        assertThat(TestWorkItem1Handler.failureContextUnitOfWork).isNull()
        verify(TestWorkItem1Handler.successContextUnitOfWork!!).commit()
    }

    @Test
    fun `processSingleItem - Shall set MDC values during handling`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = false),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        assertThat(TestWorkItem1Handler.mdc?.get(TestAdapterSettings.mdcQueueIdLabel))
            .isEqualTo(stubbedWorkItem.queueId)
        assertThat(TestWorkItem1Handler.mdc?.get(TestAdapterSettings.mdcItemIdLabel))
            .isEqualTo(stubbedWorkItem.itemId)
        assertThat(TestWorkItem1Handler.mdc?.get(TestAdapterSettings.mdcItemTypeLabel))
            .isEqualTo(stubbedWorkItem.type)
        assertThat(TestWorkItem1Handler.mdc?.get(TestAdapterSettings.mdcHandlerClassLabel))
            .isEqualTo(TestWorkItem1Handler::class.simpleName)
    }

    @Test
    fun `processSingleItem - Shall not modify item state if cancelled`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = false),
            queueId = "queue-1"
        )!!

        whenever(
            workItemsDAO.getById(
                eq(stubbedWorkItem.queueId.toWorkItemQueueId()),
                eq(stubbedWorkItem.itemId.toWorkItemId())
            )
        ).thenReturn(stubbedWorkItem.copy(state = WorkItemState.CANCELLED.dbOrdinal))

        val successUnitOfWork = mock<UnitOfWork>()
        val failureUnitOfWork = mock<UnitOfWork>()
        whenever(unitOfWorkFactory.create()).thenReturn(successUnitOfWork, failureUnitOfWork)

        whenever(successUnitOfWork.commit()).thenThrow(RuntimeException("unable to commit due to state change"))

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        verify(workItemsDAO, never()).markInProgressItemAsFailed(any(), any(), any(), any(), any(), any())
        verify(workItemsDAO, never()).rescheduleInProgressItem(any(), any(), any(), any(), any(), any())
        assertThat(TestWorkItem1Handler.failureError).isNull()
    }

    @Test
    fun `processSingleItem - Shall not modify item state if deleted`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = false),
            queueId = "queue-1"
        )!!

        whenever(
            workItemsDAO.getById(
                eq(stubbedWorkItem.queueId.toWorkItemQueueId()),
                eq(stubbedWorkItem.itemId.toWorkItemId())
            )
        ).thenReturn(null)

        val successUnitOfWork = mock<UnitOfWork>()
        val failureUnitOfWork = mock<UnitOfWork>()
        whenever(unitOfWorkFactory.create()).thenReturn(successUnitOfWork, failureUnitOfWork)

        whenever(successUnitOfWork.commit()).thenThrow(RuntimeException("unable to commit due to state change"))

        processor.processSingleItem(stubbedWorkItem.queueId.toWorkItemQueueId(), typeHandlerMap)

        verify(workItemsDAO, never()).markInProgressItemAsFailed(any(), any(), any(), any(), any(), any())
        verify(workItemsDAO, never()).rescheduleInProgressItem(any(), any(), any(), any(), any(), any())
        assertThat(TestWorkItem1Handler.failureError).isNull()
    }

    private suspend fun <T> stubNextWorkItem(item: T, queueId: String): WorkItemDbEntity? {
        val stubbedWorkItem = item?.let {
            createWorkItemDbEntity(
                item = it,
                queueId = "queue-1"
            )
        }

        whenever(workQueue.takeNext(eq(queueId.toWorkItemQueueId()), any(), any()))
            .thenReturn(stubbedWorkItem)

        if (stubbedWorkItem != null) {
            whenever(workItemsDAO.getById(eq(queueId.toWorkItemQueueId()), eq(stubbedWorkItem.itemId.toWorkItemId())))
                .thenReturn(stubbedWorkItem)
        }

        return stubbedWorkItem
    }

    private val typeHandlerMap = mapOf<String, WorkItemHandler<*>>(
        TestWorkItem1::class.simpleName!! to TestWorkItem1Handler(),
        UnparsableWorkItem::class.simpleName!! to UnparsableWorkItemHandler(),
    )

    private fun <T : Any> createWorkItemDbEntity(
        item: T,
        queueId: String
    ): WorkItemDbEntity {
        return WorkItemDbEntity(
            uid = 1,
            itemId = "2",
            queueId = queueId,
            type = item::class.simpleName!!,
            state = WorkItemState.IN_PROGRESS.dbOrdinal,
            dataJson = CoreComponent.defaultObjectMapper.writeValueAsString(item),
            metadataJson = "{}",
            concurrencyKey = null,
            createdAt = now,
            firstScheduledFor = now,
            nextScheduledFor = now,
            stateChangedAt = now,
            watchdogOwner = watchdogId,
            watchdogTimestamp = now,
            numFailures = 0,
            expiresAt = null
        )
    }

    data class TestWorkItem1(
        val shouldFail: Boolean = false,
    )

    data class UnmappedTestWorkItem(
        val hello: String = "World!",
    )

    data class UnparsableWorkItem(
        val hello: String = "World!",
    )

    class TestWorkItem1Handler : WorkItemHandler<TestWorkItem1> {
        companion object {
            var failureAction: FailureAction = FailureAction.GiveUp
            var failureContext: WorkItemFailureContext? = null
            var failureError: Exception? = null
            var failureItem: TestWorkItem1? = null
            var successContextUnitOfWork: UnitOfWork? = null
            var failureContextUnitOfWork: UnitOfWork? = null
            var mdc: Map<String, String>? = null

            fun reset() {
                failureAction = FailureAction.GiveUp
                failureItem = null
                failureError = null
                failureContext = null
                successContextUnitOfWork = null
                failureContextUnitOfWork = null
                mdc = null
            }
        }

        override val handlerClassName: String = this::class.simpleName!!

        override val logger: Logger = LoggerCache.get(this::class)

        override fun transformItem(rawItem: WorkItemDbEntity): TestWorkItem1 {
            return CoreComponent.defaultObjectMapper.readValue<TestWorkItem1>(rawItem.dataJson)
        }

        override suspend fun onFailure(
            item: TestWorkItem1,
            error: Exception,
            context: WorkItemFailureContext
        ): FailureAction {
            failureContextUnitOfWork = context.unitOfWork
            failureItem = item
            failureContext = context
            failureError = error
            return failureAction
        }

        override suspend fun handle(item: TestWorkItem1, context: WorkItemContext) {
            successContextUnitOfWork = context.unitOfWork
            mdc = MDC.getCopyOfContextMap()

            if (item.shouldFail) {
                error("fail")
            }
        }
    }

    class UnparsableWorkItemHandler : WorkItemHandler<UnparsableWorkItem> {
        override val handlerClassName: String = this::class.simpleName!!

        override val logger: Logger = LoggerCache.get(this::class)

        override fun transformItem(rawItem: WorkItemDbEntity): UnparsableWorkItem {
            error("unable to parse item")
        }

        override suspend fun onFailure(
            item: UnparsableWorkItem,
            error: Exception,
            context: WorkItemFailureContext
        ): FailureAction {
            error("not called")
        }

        override suspend fun handle(item: UnparsableWorkItem, context: WorkItemContext) {
            error("not called")
        }
    }

    object TestAdapterSettings : DatabaseQueueAdapterSettings {
        override val metricsQueueStateMetricName: String = "test.queue.size"
        override val metricsProcessingDurationMetricName: String = "test.queue.duration.seconds"

        override val queueIdPrefix: String = "test__"

        override val mdcQueueIdLabel: String = "testQueueId"
        override val mdcItemIdLabel: String = "testItemId"
        override val mdcItemTypeLabel: String = "testType"
        override val mdcHandlerClassLabel: String = "testClass"
    }
}