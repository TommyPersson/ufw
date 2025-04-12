package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkQueueState
import io.tpersson.ufw.databasequeue.internal.WorkQueueDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkQueuesDAO
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import kotlin.test.Test

internal class CachingQueueStateCheckerImplTest {

    private lateinit var workQueuesDAO: WorkQueuesDAO
    private lateinit var clock: TestClock

    private lateinit var checker: CachingQueueStateCheckerImpl

    @BeforeEach
    fun setUp() {
        workQueuesDAO = mock<WorkQueuesDAO>()
        clock = TestClock()

        checker = CachingQueueStateCheckerImpl(
            workQueuesDAO = workQueuesDAO,
            clock = clock
        )
    }

    @Test
    fun `isQueuePaused - Forwards initial request to DTO, then reads cached value`(): Unit = runBlocking {
        val queueId = WorkItemQueueId("test-queue")

        whenever(workQueuesDAO.getWorkQueue(queueId)).thenReturn(
            WorkQueueDbEntity(
                queueId = queueId.value,
                state = WorkQueueState.PAUSED.name,
                stateChangedAt = clock.instant()
            )
        )

        assertThat(checker.isQueuePaused(queueId)).isTrue()
        assertThat(checker.isQueuePaused(queueId)).isTrue()

        verify(workQueuesDAO, times(1)).getWorkQueue(queueId)
    }

    @Test
    fun `isQueuePaused - Invalidates cache after a short time`(): Unit = runBlocking {
        val queueId = WorkItemQueueId("test-queue")

        whenever(workQueuesDAO.getWorkQueue(queueId)).thenReturn(
            WorkQueueDbEntity(
                queueId = queueId.value,
                state = WorkQueueState.PAUSED.name,
                stateChangedAt = clock.instant()
            )
        )

        assertThat(checker.isQueuePaused(queueId)).isTrue()

        clock.advance(Duration.ofSeconds(6))

        assertThat(checker.isQueuePaused(queueId)).isTrue()

        verify(workQueuesDAO, times(2)).getWorkQueue(queueId)
    }
}