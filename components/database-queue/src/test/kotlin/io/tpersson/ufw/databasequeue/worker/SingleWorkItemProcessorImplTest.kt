package io.tpersson.ufw.databasequeue.worker

import com.fasterxml.jackson.module.kotlin.readValue
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.*
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.slf4j.MDC
import java.time.Instant
import java.time.InstantSource
import java.util.*

internal class SingleWorkItemProcessorImplTest {

    private lateinit var unitOfWorkFactory: UnitOfWorkFactory
    private lateinit var workItemFailuresDAO: WorkItemFailuresDAO
    private lateinit var workItemsDAO: WorkItemsDAO
    private lateinit var watchdogId: String

    private lateinit var clock: InstantSource
    private lateinit var now: Instant

    private lateinit var processor: SingleWorkItemProcessorImpl

    private val config = DatabaseQueueConfig()

    @BeforeEach
    fun setUp() {
        watchdogId = UUID.randomUUID().toString()
        workItemsDAO = mock()
        workItemFailuresDAO = mock()

        unitOfWorkFactory = mock()
        whenever(unitOfWorkFactory.create()).then { mock<UnitOfWork>() }

        now = Instant.now()

        clock = mock()
        whenever(clock.instant()).then { now }

        TestWorkItem1Handler.reset()

        processor = SingleWorkItemProcessorImpl(
            watchdogId = watchdogId,
            workItemsDAO = workItemsDAO,
            workItemFailuresDAO = workItemFailuresDAO,
            unitOfWorkFactory = unitOfWorkFactory,
            clock = clock,
            mdcLabels = TestMdcLabels,
            config = config,
        )
    }

    @Test
    fun `processSingleItem - Returns false if no item was taken from the queue`(): Unit = runBlocking {
        stubNextWorkItem(
            item = null,
            queueId = "queue-1"
        )

        val result = processor.processSingleItem("queue-1", emptyMap())

        assertThat(result).isFalse()

        verify(workItemsDAO).takeNext(eq("queue-1"), eq(watchdogId), eq(now))
    }

    @Test
    fun `processSingleItem - Returns true if an item was taken from the queue but no mapping was found`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = UnmappedTestWorkItem(),
                queueId = "queue-1"
            )!!

            val result = processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

            assertThat(result).isTrue()

            verify(workItemsDAO, never()).markInProgressItemAsSuccessful(any(), any(), any(), any(), any(), any())
            verify(workItemsDAO, never()).markInProgressItemAsFailed(any(), any(), any(), any(), any(), any())
        }

    @Test
    fun `processSingleItem - Returns true if an item was taken from the queue and successfully processed`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = false),
                queueId = "queue-1"
            )!!

            val result = processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

            assertThat(result).isTrue()
        }

    @Test
    fun `processSingleItem - Returns true if an item was taken from the queue and failing processed`(): Unit =
        runBlocking {
            val stubbedWorkItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = true),
                queueId = "queue-1"
            )!!

            val result = processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

            assertThat(result).isTrue()
        }

    @Test
    fun `processSingleItem - Returns mark item as successful on successful processing`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

        verify(workItemsDAO).markInProgressItemAsSuccessful(
            queueId = eq(stubbedWorkItem.queueId),
            itemId = eq(stubbedWorkItem.itemId),
            expiresAt = eq(now.plus(config.successfulItemExpirationDelay)),
            watchdogId = eq(watchdogId),
            now = eq(now),
            unitOfWork = same(TestWorkItem1Handler.successContextUnitOfWork!!)
        )

        verify(TestWorkItem1Handler.successContextUnitOfWork!!).commit()
    }

    @Test
    fun `processSingleItem - Returns mark item as failed on failed processing`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = true),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

        verify(workItemsDAO).markInProgressItemAsFailed(
            queueId = eq(stubbedWorkItem.queueId),
            itemId = eq(stubbedWorkItem.itemId),
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

            processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

            verify(workItemsDAO).rescheduleInProgressItem(
                queueId = eq(stubbedWorkItem.queueId),
                itemId = eq(stubbedWorkItem.itemId),
                watchdogId = eq(watchdogId),
                scheduleFor = same(now),
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

            processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

            verify(workItemsDAO).rescheduleInProgressItem(
                queueId = eq(stubbedWorkItem.queueId),
                itemId = eq(stubbedWorkItem.itemId),
                watchdogId = eq(watchdogId),
                scheduleFor = same(rescheduleAt),
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

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

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

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

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

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

        val unitOfWorkCaptor = argumentCaptor<UnitOfWork>()

        verify(workItemsDAO).markInProgressItemAsFailed(
            queueId = eq(stubbedWorkItem.queueId),
            itemId = eq(stubbedWorkItem.itemId),
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

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

        verify(TestWorkItem1Handler.successContextUnitOfWork!!, never()).commit()
        verify(TestWorkItem1Handler.failureContextUnitOfWork!!).commit()
    }

    @Test
    fun `processSingleItem - Shall not commit UnitOfWork in failure context on success`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = false),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

        assertThat(TestWorkItem1Handler.failureContextUnitOfWork).isNull()
        verify(TestWorkItem1Handler.successContextUnitOfWork!!).commit()
    }

    @Test
    fun `processSingleItem - Shall set MDC values during handling`(): Unit = runBlocking {
        val stubbedWorkItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = false),
            queueId = "queue-1"
        )!!

        processor.processSingleItem(stubbedWorkItem.queueId, typeHandlerMap)

        assertThat(TestWorkItem1Handler.mdc?.get(TestMdcLabels.queueIdLabel)).isEqualTo(stubbedWorkItem.queueId)
        assertThat(TestWorkItem1Handler.mdc?.get(TestMdcLabels.itemIdLabel)).isEqualTo(stubbedWorkItem.itemId)
        assertThat(TestWorkItem1Handler.mdc?.get(TestMdcLabels.itemTypeLabel)).isEqualTo(stubbedWorkItem.type)
        assertThat(TestWorkItem1Handler.mdc?.get(TestMdcLabels.handlerClassLabel)).isEqualTo(TestWorkItem1Handler::class.simpleName)
    }

    private suspend fun <T> stubNextWorkItem(item: T, queueId: String): WorkItemDbEntity? {
        val stubbedWorkItem = item?.let {
            createWorkItemDbEntity(
                item = it,
                queueId = "queue-1"
            )
        }

        whenever(workItemsDAO.takeNext(eq(queueId), any(), any()))
            .thenReturn(stubbedWorkItem)

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

    object TestMdcLabels : DatabaseQueueMdcLabels {
        override val queueIdLabel: String = "testQueueId"
        override val itemIdLabel: String = "testItemId"
        override val itemTypeLabel: String = "testType"
        override val handlerClassLabel: String = "testClass"
    }
}