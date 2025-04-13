package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class DatabaseQueueExpiredItemReaperTest {
    @Test
    fun `runOnce - Calls dao with correct timestamp`(): Unit = runBlocking {
        val clock = TestClock()
        val workItemsDAOMock = mock<WorkItemsDAO>()

        whenever(workItemsDAOMock.deleteExpiredItems(any())).thenReturn(1)

        val reaper = DatabaseQueueExpiredItemReaper(
            workItemsDAO = workItemsDAOMock,
            clock = clock,
            configProvider = ConfigProvider.empty()
        )

        reaper.runOnce()

        verify(workItemsDAOMock).deleteExpiredItems(eq(clock.instant()))
    }

}