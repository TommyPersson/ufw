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
import java.time.Instant
import java.time.InstantSource
import java.util.*

internal class SingleWorkItemProcessorTest {

    private lateinit var unitOfWork: UnitOfWork
    private lateinit var unitOfWorkFactory: UnitOfWorkFactory
    private lateinit var workItemFailuresDAO: WorkItemFailuresDAO
    private lateinit var workItemsDAO: WorkItemsDAO
    private lateinit var watchdogId: String

    private lateinit var clock: InstantSource
    private lateinit var now: Instant

    private lateinit var processor: SingleWorkItemProcessor

    private val config = DatabaseQueueConfig()

    @BeforeEach
    fun setUp() {
        watchdogId = UUID.randomUUID().toString()
        workItemsDAO = mock()
        workItemFailuresDAO = mock()

        unitOfWorkFactory = mock()
        unitOfWork = mock()
        whenever(unitOfWorkFactory.create()).thenReturn(unitOfWork)

        now = Instant.now()

        clock = mock()
        whenever(clock.instant()).then { now }

        TestWorkItem1Handler.reset()

        processor = SingleWorkItemProcessor(
            watchdogId = watchdogId,
            workItemsDAO = workItemsDAO,
            workItemFailuresDAO = workItemFailuresDAO,
            unitOfWorkFactory = unitOfWorkFactory,
            clock = clock,
            config = config
        )
    }

    @Test
    fun `processSingleItem - Returns false if no item was taken from the queue`(): Unit = runBlocking {
        val queueId = "queue-1"

        stubNextWorkItem(
            item = null,
            queueId = queueId
        )

        val result = processor.processSingleItem(queueId, emptyMap())

        assertThat(result).isFalse()

        verify(workItemsDAO).takeNext(eq(queueId), eq(watchdogId), eq(now))
    }

    @Test
    fun `processSingleItem - Returns true if an item was taken from the queue but no mapping was found`(): Unit =
        runBlocking {
            val queueId = "queue-1"

            stubNextWorkItem(
                item = UnmappedTestWorkItem(),
                queueId = queueId
            )

            val result = processor.processSingleItem(queueId, typeHandlerMap)

            assertThat(result).isTrue()

            verify(workItemsDAO, never()).markInProgressItemAsSuccessful(any(), any(), any(), any(), any(), any())
            verify(workItemsDAO, never()).markInProgressItemAsFailed(any(), any(), any(), any(), any(), any())
        }

    @Test
    fun `processSingleItem - Returns true if an item was taken from the queue and successfully processed`(): Unit =
        runBlocking {
            val queueId = "queue-1"

            stubNextWorkItem(
                item = TestWorkItem1(shouldFail = false),
                queueId = queueId
            )

            val result = processor.processSingleItem(queueId, typeHandlerMap)

            assertThat(result).isTrue()
        }

    @Test
    fun `processSingleItem - Returns true if an item was taken from the queue and failing processed`(): Unit =
        runBlocking {
            val queueId = "queue-1"

            stubNextWorkItem(
                item = TestWorkItem1(shouldFail = true),
                queueId = queueId
            )

            val result = processor.processSingleItem(queueId, typeHandlerMap)

            assertThat(result).isTrue()
        }

    @Test
    fun `processSingleItem - Returns mark item as successful on successful processing`(): Unit = runBlocking {
        val queueId = "queue-1"

        val workItem = stubNextWorkItem(
            item = TestWorkItem1(),
            queueId = queueId
        )!!

        processor.processSingleItem(queueId, typeHandlerMap)

        verify(workItemsDAO).markInProgressItemAsSuccessful(
            queueId = eq(queueId),
            itemId = eq(workItem.itemId),
            expiresAt = eq(now.plus(config.successfulItemExpirationDelay)),
            watchdogId = eq(watchdogId),
            now = eq(now),
            unitOfWork = same(unitOfWork)
        )

        verify(unitOfWork).commit()
    }

    @Test
    fun `processSingleItem - Returns mark item as failed on failed processing`(): Unit = runBlocking {
        val queueId = "queue-1"

        val workItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = true),
            queueId = queueId
        )!!

        processor.processSingleItem(queueId, typeHandlerMap)

        verify(workItemsDAO).markInProgressItemAsFailed(
            queueId = eq(queueId),
            itemId = eq(workItem.itemId),
            expiresAt = eq(now.plus(config.failedItemExpirationDelay)),
            watchdogId = eq(watchdogId),
            now = eq(now),
            unitOfWork = same(unitOfWork)
        )

        verify(unitOfWork).commit()
    }

    @Test
    fun `processSingleItem - Shall reschedule failed items according to FailureAction_RescheduleNow`(): Unit =
        runBlocking {
            val queueId = "queue-1"

            val workItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = true),
                queueId = queueId
            )!!

            TestWorkItem1Handler.failureAction = FailureAction.RescheduleNow

            processor.processSingleItem(queueId, typeHandlerMap)

            verify(workItemsDAO).rescheduleInProgressItem(
                queueId = eq(queueId),
                itemId = eq(workItem.itemId),
                watchdogId = eq(watchdogId),
                scheduleFor = same(now),
                now = eq(now),
                unitOfWork = same(unitOfWork),
            )
        }

    @Test
    fun `processSingleItem - Shall reschedule failed items according to FailureAction_RescheduleAt`(): Unit =
        runBlocking {
            val queueId = "queue-1"

            val workItem = stubNextWorkItem(
                item = TestWorkItem1(shouldFail = true),
                queueId = queueId
            )!!

            val rescheduleAt = Instant.now()

            TestWorkItem1Handler.failureAction = FailureAction.RescheduleAt(rescheduleAt)

            processor.processSingleItem(queueId, typeHandlerMap)

            verify(workItemsDAO).rescheduleInProgressItem(
                queueId = eq(queueId),
                itemId = eq(workItem.itemId),
                watchdogId = eq(watchdogId),
                scheduleFor = same(rescheduleAt),
                now = eq(now),
                unitOfWork = same(unitOfWork),
            )
        }

    @Test
    fun `processSingleItem - Shall store a failure entry whenever an item fails processing`(): Unit = runBlocking {
        val queueId = "queue-1"

        val workItem = stubNextWorkItem(
            item = TestWorkItem1(shouldFail = true),
            queueId = queueId
        )!!

        val rescheduleAt = Instant.now()

        TestWorkItem1Handler.failureAction = FailureAction.RescheduleAt(rescheduleAt)

        processor.processSingleItem(queueId, typeHandlerMap)

        verify(workItemFailuresDAO).insertFailure(
            failure = argWhere {
                it.id.isNotEmpty() &&
                        it.itemUid == workItem.uid &&
                        it.timestamp == now &&
                        it.errorType == "IllegalStateException" &&
                        it.errorMessage == "fail" &&
                        it.errorStackTrace.contains("TestWorkItem1Handler")
            },
            unitOfWork = same(unitOfWork),
        )
    }

    @Test
    fun `processSingleItem - Shall provide a data to the failure handler`(): Unit = runBlocking {
        val queueId = "queue-1"

        val workItem = TestWorkItem1(shouldFail = true)

        stubNextWorkItem(
            item = workItem,
            queueId = queueId
        )!!

        processor.processSingleItem(queueId, typeHandlerMap)

        assertThat(TestWorkItem1Handler.failureItem).isEqualTo(workItem)

        assertThat(TestWorkItem1Handler.failureError?.message).isEqualTo("fail")

        assertThat(TestWorkItem1Handler.failureContext?.failureCount).isEqualTo(1)
        assertThat(TestWorkItem1Handler.failureContext?.clock).isSameAs(clock)
        assertThat(TestWorkItem1Handler.failureContext?.timestamp).isSameAs(now)
    }

    private suspend fun <T> stubNextWorkItem(item: T, queueId: String): WorkItemDbEntity? {
        val workItemDbEntity = item?.let {
            createWorkItemDbEntity(
                item = it,
                queueId = "queue-1"
            )
        }

        whenever(workItemsDAO.takeNext(eq(queueId), any(), any()))
            .thenReturn(workItemDbEntity)

        return workItemDbEntity
    }

    private val typeHandlerMap = mapOf<String, WorkItemHandler<*>>(
        TestWorkItem1::class.simpleName!! to TestWorkItem1Handler()
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
            state = WorkItemState.IN_PROGRESS,
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

    class TestWorkItem1Handler : WorkItemHandler<TestWorkItem1> {
        companion object {
            var failureAction: FailureAction = FailureAction.GiveUp
            var failureContext: WorkItemFailureContext? = null
            var failureError: Exception? = null
            var failureItem: TestWorkItem1? = null

            fun reset() {
                failureAction = FailureAction.GiveUp
                failureItem = null
                failureError = null
                failureContext = null
            }
        }

        override fun transformItem(rawItem: WorkItemDbEntity): TestWorkItem1 {
            return CoreComponent.defaultObjectMapper.readValue<TestWorkItem1>(rawItem.dataJson)
        }

        override suspend fun onFailure(
            item: TestWorkItem1,
            error: Exception,
            context: WorkItemFailureContext
        ): FailureAction {
            failureItem = item
            failureContext = context
            failureError = error
            return failureAction
        }

        override suspend fun handle(item: TestWorkItem1) {
            if (item.shouldFail) {
                error("fail")
            }
        }
    }
}