package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant

internal class DatabaseQueueHangedItemReschedulerTest {

    private lateinit var workItemsDAO: WorkItemsDAO

    private lateinit var rescheduler: DatabaseQueueHangedItemRescheduler

    private lateinit var clock: TestClock

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        workItemsDAO = mock()

        clock = TestClock()

        whenever(workItemsDAO.rescheduleAllHangedItems(any(), any(), any())).thenReturn(0)

        rescheduler = DatabaseQueueHangedItemRescheduler(
            workItemsDAO = workItemsDAO,
            clock = clock,
            config = DatabaseQueueConfig(
                watchdogTimeout = Duration.ofMinutes(5)
            )
        )
    }

    @Test
    fun `runOnce - Reschedules items according to config`(): Unit = runBlocking {
        val now = Instant.parse("2022-02-02T02:02:00Z")
        clock.reset(now)

        rescheduler.runOnce()

        verify(workItemsDAO).rescheduleAllHangedItems(
            rescheduleIfWatchdogOlderThan = now.minus(Duration.ofMinutes(5)),
            scheduleFor = now,
            now = now,
        )
    }
}