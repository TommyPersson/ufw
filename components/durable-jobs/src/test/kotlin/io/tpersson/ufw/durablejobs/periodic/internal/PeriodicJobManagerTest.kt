package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobStateData
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.*
import java.time.Instant

internal class PeriodicJobManagerTest {

    private lateinit var periodicJobScheduler: PeriodicJobScheduler
    private lateinit var periodicJobsDAO: PeriodicJobsDAO
    private lateinit var clock: TestClock

    private lateinit var manager: PeriodicJobManager

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        periodicJobScheduler = mock<PeriodicJobScheduler>()
        periodicJobsDAO = mock<PeriodicJobsDAO>()
        clock = TestClock()

        manager = PeriodicJobManager(
            periodicJobSpecsProvider = mock(),
            periodicJobScheduler = periodicJobScheduler,
            periodicJobsDAO = periodicJobsDAO,
            clock = clock
        )
    }

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        manager.stop()
    }

    @Test
    fun `runOnce - Invokes scheduler`(): Unit = runBlocking {
        manager.runOnce()

        verify(periodicJobScheduler).scheduleAnyPendingJobs()
    }

    @Test
    fun `getState - Invokes DAO`(): Unit = runBlocking {
        val expectedList = listOf(mock<PeriodicJobStateData>())

        whenever(periodicJobsDAO.getAll(any())).thenReturn(
            PaginatedList(
                items = expectedList,
                options = PaginationOptions.DEFAULT,
                hasMoreItems = false
            )
        )

        val state = manager.getState()

        assertThat(state).isSameAs(expectedList)
    }

    @Test
    fun `scheduleJobNow - Invokes scheduler`(): Unit = runBlocking {
        val now = Instant.now()
        val periodicJobSpec = mock<PeriodicJobSpec<*>>()

        manager.scheduleJobNow(periodicJobSpec, now = now)

        verify(periodicJobScheduler).scheduleJobNow(eq(periodicJobSpec), eq(now))
    }

    @Test
    fun `scheduleJobNow - Invokes scheduler, defers to default clock if not time is provided`(): Unit = runBlocking {
        clock.reset(Instant.now().plusSeconds(5))

        val periodicJobSpec = mock<PeriodicJobSpec<*>>()

        manager.scheduleJobNow(periodicJobSpec)

        verify(periodicJobScheduler).scheduleJobNow(eq(periodicJobSpec), eq(clock.instant()))
    }
}