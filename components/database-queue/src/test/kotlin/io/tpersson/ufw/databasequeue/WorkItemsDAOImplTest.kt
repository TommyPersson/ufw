package io.tpersson.ufw.databasequeue

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.exceptions.MinimumAffectedRowsException
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemEvent
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class WorkItemsDAOImplTest {

    companion object {
        @JvmStatic
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15"))
            .withReuse(true)
            .also {
                Startables.deepStart(it).join()
            }

        val config = HikariConfig().also {
            it.jdbcUrl = postgres.jdbcUrl
            it.username = postgres.username
            it.password = postgres.password
            it.maximumPoolSize = 5
            it.isAutoCommit = false
        }

        val testClock = TestClock()

        val ufw = UFW.build {
            core {
                clock = testClock
            }
            database {
                dataSource = HikariDataSource(config)
            }
            databaseQueue {
            }
        }

        val database = ufw.database.database
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory

        init {
            ufw.database.migrator.run()
        }
    }

    private lateinit var dao: WorkItemsDAOImpl

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        dao = WorkItemsDAOImpl(database, ufw.core.objectMapper)
        dao.debugTruncate()
    }

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        dao.debugTruncate()
    }

    @Test
    fun `scheduleNewItem - Inserts an item in a SCHEDULED state`(): Unit = runBlocking {
        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        dao.scheduleNewItem(
            newItem = NewWorkItem(
                queueId = "testQueueId".toWorkItemQueueId(),
                itemId = "testId".toWorkItemId(),
                type = "testType",
                metadataJson = "metadataJson",
                concurrencyKey = "concurrencyKey",
                dataJson = "dataJson",
                scheduleFor = scheduleFor,
            ),
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById("testQueueId".toWorkItemQueueId(), "testId".toWorkItemId())!!

        assertThat(item.uid).isGreaterThan(0)
        assertThat(item.itemId).isEqualTo("testId")
        assertThat(item.queueId).isEqualTo("testQueueId")
        assertThat(item.type).isEqualTo("testType")
        assertThat(item.dataJson).isEqualTo("dataJson")
        assertThat(item.state).isEqualTo(WorkItemState.SCHEDULED.dbOrdinal)
        assertThat(item.metadataJson).isEqualTo("metadataJson")
        assertThat(item.concurrencyKey).isEqualTo("concurrencyKey")
        assertThat(item.createdAt).isEqualTo(now)
        assertThat(item.firstScheduledFor).isEqualTo(scheduleFor)
        assertThat(item.nextScheduledFor).isEqualTo(scheduleFor)
        assertThat(item.stateChangedAt).isEqualTo(now)
        assertThat(item.watchdogOwner).isNull()
        assertThat(item.watchdogTimestamp).isNull()
        assertThat(item.expiresAt).isNull()
    }

    @Test
    fun `scheduleNewItem - Shall record state transition events`(): Unit = runBlocking {
        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        dao.scheduleNewItem(
            newItem = NewWorkItem(
                queueId = "testQueueId".toWorkItemQueueId(),
                itemId = "testId".toWorkItemId(),
                type = "testType",
                metadataJson = "metadataJson",
                concurrencyKey = "concurrencyKey",
                dataJson = "dataJson",
                scheduleFor = scheduleFor,
            ),
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById("testQueueId".toWorkItemQueueId(), "testId".toWorkItemId())!!

        assertTransitionEvents(
            item,
            { it is WorkItemEvent.Scheduled && it.timestamp == now && it.scheduledFor == scheduleFor }
        )
    }

    @Test
    fun `listAllItems - Shall return items in the correct queue`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "1", queueId = "queue-1"))
        debugInsertItems(makeWorkItem(itemId = "2", queueId = "queue-1"))
        debugInsertItems(makeWorkItem(itemId = "3", queueId = "queue-2"))

        val itemIds1 = dao.listAllItems(queueId = "queue-1".toWorkItemQueueId())
            .items.map { it.itemId }.toSet()

        val itemIds2 = dao.listAllItems(queueId = "queue-2".toWorkItemQueueId())
            .items.map { it.itemId }.toSet()

        assertThat(itemIds1).isEqualTo(setOf("1", "2"))
        assertThat(itemIds2).isEqualTo(setOf("3"))
    }

    @Test
    fun `listAllItems - Shall return items in the correct queue & state`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "1", queueId = "queue-1", state = WorkItemState.IN_PROGRESS.dbOrdinal))
        debugInsertItems(makeWorkItem(itemId = "2", queueId = "queue-1", state = WorkItemState.SUCCESSFUL.dbOrdinal))
        debugInsertItems(makeWorkItem(itemId = "3", queueId = "queue-2", state = WorkItemState.IN_PROGRESS.dbOrdinal))

        val itemIds1 = dao.listAllItems(queueId = "queue-1".toWorkItemQueueId(), state = WorkItemState.IN_PROGRESS)
            .items.map { it.itemId }.toSet()

        val itemIds2 = dao.listAllItems(queueId = "queue-1".toWorkItemQueueId(), state = WorkItemState.SUCCESSFUL)
            .items.map { it.itemId }.toSet()

        val itemIds3 = dao.listAllItems(queueId = "queue-2".toWorkItemQueueId(), state = WorkItemState.SUCCESSFUL)
            .items.map { it.itemId }.toSet()

        assertThat(itemIds1).isEqualTo(setOf("1"))
        assertThat(itemIds2).isEqualTo(setOf("2"))
        assertThat(itemIds3).isEqualTo(emptySet<String>())
    }

    @Test
    fun `takeNext - Takes a SCHEDULED item and moves it to IN_PROGRESS`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId", queueId = "testQueueId"))
        testClock.advance(Duration.ofMinutes(2))

        val now = testClock.instant().truncatedTo(ChronoUnit.MILLIS)

        run {
            val item = dao.takeNext("testQueueId".toWorkItemQueueId(), watchdogId = "testWatchdog", now = now)

            assertThat(item?.state).isEqualTo(WorkItemState.IN_PROGRESS.dbOrdinal)
            assertThat(item?.stateChangedAt).isEqualTo(now)
            assertThat(item?.watchdogOwner).isEqualTo("testWatchdog")
            assertThat(item?.watchdogTimestamp).isEqualTo(now)
        }

        run {
            val item = dao.debugListAllItems().items.first { it.itemId == "testId" }

            assertThat(item.state).isEqualTo(WorkItemState.IN_PROGRESS.dbOrdinal)
            assertThat(item.stateChangedAt).isEqualTo(now)
            assertThat(item.watchdogOwner).isEqualTo("testWatchdog")
            assertThat(item.watchdogTimestamp).isEqualTo(now)
        }
    }

    @Test
    fun `takeNext - Shall not take an item with the same concurrency as an already IN_PROGRESS item`(): Unit =
        runBlocking {
            debugInsertItems(makeWorkItem(itemId = "testId1", concurrencyKey = "concurrencyKey"))
            testClock.advance(Duration.ofMinutes(2))
            debugInsertItems(makeWorkItem(itemId = "testId2", concurrencyKey = "concurrencyKey"))
            testClock.advance(Duration.ofMinutes(2))

            val now = testClock.instant().truncatedTo(ChronoUnit.MILLIS)

            run {
                val item = dao.takeNext("testQueueId".toWorkItemQueueId(), watchdogId = "testWatchdog", now = now)

                assertThat(item?.itemId).isEqualTo("testId1")
            }

            run {
                val item = dao.takeNext("testQueueId".toWorkItemQueueId(), watchdogId = "testWatchdog", now = now)

                assertThat(item).isNull()
            }
        }

    @Test
    fun `takeNext - Shall not take an item scheduled for the future`(): Unit = runBlocking {
        val now = testClock.instant().truncatedTo(ChronoUnit.MILLIS)
        val scheduleFor = now.plus(Duration.ofMinutes(1))

        debugInsertItems(
            makeWorkItem(
                itemId = "testId1",
                firstScheduledFor = scheduleFor,
                nextScheduledFor = scheduleFor
            )
        )

        val item = dao.takeNext("testQueueId".toWorkItemQueueId(), watchdogId = "testWatchdog", now = now)

        assertThat(item).isNull()
    }

    @Test
    fun `takeNext - Shall record state transition event`(): Unit = runBlocking {
        // Arrange
        debugInsertItems(makeWorkItem(itemId = "testId1", concurrencyKey = "concurrencyKey"))

        testClock.advance(Duration.ofMinutes(2))
        val now = testClock.dbNow

        // Act
        val item = dao.takeNext("testQueueId".toWorkItemQueueId(), watchdogId = "testWatchdog", now = now)!!

        // Assert
        assertTransitionEvents(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            { it is WorkItemEvent.Taken && it.timestamp == now },
        )
    }

    @Test
    fun `markInProgressItemAsSuccessful - Shall record state transition events`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        assertTransitionEvents(
            item,
            { it is WorkItemEvent.Taken },
            { it is WorkItemEvent.Successful && it.timestamp == now }
        )
    }

    @Test
    fun `markInProgressItemAsSuccessful - Shall move an IN_PROGRESS item to SUCCESSFUL`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.debugListAllItems().items.first { it.itemId == "testId1" }

        assertThat(item2.state).isEqualTo(WorkItemState.SUCCESSFUL.dbOrdinal)
        assertThat(item2.watchdogOwner).isNull()
        assertThat(item2.watchdogTimestamp).isNull()
        assertThat(item2.expiresAt).isEqualTo(expiresAt)
        assertThat(item2.stateChangedAt).isEqualTo(now)
        assertThat(item2.nextScheduledFor).isNull()
    }

    @Test
    fun `markInProgressItemAsSuccessful - Shall fail UnitOfWork if the watchdog is incorrect`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "notTheSameWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `markInProgressItemAsSuccessful - Shall fail UnitOfWork if the item is not IN_PROGRESS`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1", queueId = "testQueueId", watchdogOwner = "watchdog"))
        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = "testQueueId".toWorkItemQueueId(),
            itemId = "testId1".toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "watchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `markInProgressItemAsFailed - Shall move an IN_PROGRESS item to FAILED`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.debugListAllItems().items.first { it.itemId == "testId1" }

        assertThat(item2.state).isEqualTo(WorkItemState.FAILED.dbOrdinal)
        assertThat(item2.watchdogOwner).isNull()
        assertThat(item2.watchdogTimestamp).isNull()
        assertThat(item2.expiresAt).isEqualTo(expiresAt)
        assertThat(item2.stateChangedAt).isEqualTo(now)
        assertThat(item2.nextScheduledFor).isNull()
    }

    @Test
    fun `markInProgressItemAsFailed - Shall record state transition events`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        assertTransitionEvents(
            item,
            { it is WorkItemEvent.Taken },
            { it is WorkItemEvent.Failed && it.timestamp == now }
        )
    }


    @Test
    fun `markInProgressItemAsFailed - Shall increment the number of failures`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1", numFailures = 2))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.debugListAllItems().items.first { it.itemId == item.itemId }

        assertThat(item2.numFailures).isEqualTo(3)
    }

    @Test
    fun `markInProgressItemAsFailed - Shall fail UnitOfWork if the watchdog is incorrect`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "notTheSameWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `markInProgressItemAsFailed - Shall fail UnitOfWork if the item is not IN_PROGRESS`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1", queueId = "testQueueId", watchdogOwner = "watchdog"))
        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = "testQueueId".toWorkItemQueueId(),
            itemId = "testId1".toWorkItemId(),
            expiresAt = expiresAt,
            watchdogId = "watchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `rescheduleInProgressItem - Shall move an IN_PROGRESS item to SCHEDULED`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.rescheduleInProgressItem(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            scheduleFor = scheduleFor,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.debugListAllItems().items.first { it.itemId == "testId1" }

        assertThat(item2.state).isEqualTo(WorkItemState.SCHEDULED.dbOrdinal)
        assertThat(item2.watchdogOwner).isNull()
        assertThat(item2.watchdogTimestamp).isNull()
        assertThat(item2.expiresAt).isNull()
        assertThat(item2.stateChangedAt).isEqualTo(now)
        assertThat(item2.nextScheduledFor).isEqualTo(scheduleFor)
    }

    @Test
    fun `rescheduleInProgressItem - Shall increment the number of failures`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1", numFailures = 2))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.rescheduleInProgressItem(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            scheduleFor = scheduleFor,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.debugListAllItems().items.first { it.itemId == item.itemId }

        assertThat(item2.numFailures).isEqualTo(3)
    }

    @Test
    fun `rescheduleInProgressItem - Shall record state transition events`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        unitOfWorkFactory.use { uow ->
            dao.rescheduleInProgressItem(
                queueId = item.queueId.toWorkItemQueueId(),
                itemId = item.itemId.toWorkItemId(),
                scheduleFor = scheduleFor,
                watchdogId = "testWatchdog",
                now = now,
                unitOfWork = uow
            )
        }

        assertTransitionEvents(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            { it is WorkItemEvent.Taken },
            { it is WorkItemEvent.Failed && it.timestamp == now },
            { it is WorkItemEvent.AutomaticallyRescheduled && it.timestamp == now && it.scheduledFor == scheduleFor }
        )
    }

    @Test
    fun `rescheduleInProgressItem - Shall fail UnitOfWork if the watchdog is incorrect`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId".toWorkItemQueueId(),
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.rescheduleInProgressItem(
            queueId = item.queueId.toWorkItemQueueId(),
            itemId = item.itemId.toWorkItemId(),
            scheduleFor = scheduleFor,
            watchdogId = "notTheSameWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `rescheduleInProgressItem - Shall fail UnitOfWork if the item is not IN_PROGRESS`(): Unit = runBlocking {
        debugInsertItems(makeWorkItem(itemId = "testId1", queueId = "testQueueId", watchdogOwner = "watchdog"))
        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.rescheduleInProgressItem(
            queueId = "testQueueId".toWorkItemQueueId(),
            itemId = "testId1".toWorkItemId(),
            scheduleFor = scheduleFor,
            watchdogId = "watchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `manuallyRescheduleFailedItem - Shall move a FAILED work item to SCHEDULED`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.FAILED.dbOrdinal,
            nextScheduledFor = null,
        )

        debugInsertItems(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.manuallyRescheduleFailedItem(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId(),
            scheduleFor = scheduleFor,
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId()
        )!!

        assertThat(item.state).isEqualTo(WorkItemState.SCHEDULED.dbOrdinal)
        assertThat(item.stateChangedAt).isEqualTo(now)
        assertThat(item.nextScheduledFor).isEqualTo(scheduleFor)
        assertThat(item.expiresAt).isNull()
    }

    @Test
    fun `manuallyRescheduleFailedItem - Shall record state transition events`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.FAILED.dbOrdinal,
            nextScheduledFor = null,
        )

        debugInsertItems(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.manuallyRescheduleFailedItem(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId(),
            scheduleFor = scheduleFor,
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        assertTransitionEvents(
            insertedItem,
            { true }, // debug insertion
            { it is WorkItemEvent.AutomaticallyRescheduled && it.timestamp == now && it.scheduledFor == scheduleFor }
        )
    }

    @Test
    fun `manuallyRescheduleFailedItem - Shall fail UnitOfWork if the item is not FAILED`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId"
        )

        debugInsertItems(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.manuallyRescheduleFailedItem(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId(),
            scheduleFor = scheduleFor,
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `forceCancelItem - Shall move a work item to CANCELLED`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.IN_PROGRESS.dbOrdinal,
            nextScheduledFor = null,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItems(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expireAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.forceCancelItem(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId(),
            now = now,
            expireAt = expireAt,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId()
        )!!

        assertThat(item.state).isEqualTo(WorkItemState.CANCELLED.dbOrdinal)
        assertThat(item.stateChangedAt).isEqualTo(now)
        assertThat(item.nextScheduledFor).isNull()
        assertThat(item.expiresAt).isEqualTo(expireAt)
        assertThat(item.watchdogOwner).isNull()
        assertThat(item.watchdogTimestamp).isNull()
    }

    @Test
    fun `forceCancelItem - Shall record state transition events`(): Unit = runBlocking {
        // Arrange
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.IN_PROGRESS.dbOrdinal,
            nextScheduledFor = testClock.dbNow,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItems(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expireAt = testClock.dbNow.plus(Duration.ofDays(1))

        // Act
        dao.forceCancelItem(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId(),
            now = now,
            expireAt = expireAt,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        // Assert
        assertTransitionEvents(
            insertedItem,
            { it is WorkItemEvent.Cancelled && it.timestamp == now }
        )
    }

    @Test
    fun `refreshWatchdog - Shall refresh the watchdog timer if the owner matches`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.IN_PROGRESS.dbOrdinal,
            nextScheduledFor = null,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItems(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow

        dao.refreshWatchdog(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId(),
            watchdogId = "watchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId()
        )!!

        assertThat(item.watchdogOwner).isEqualTo("watchdog")
        assertThat(item.watchdogTimestamp).isEqualTo(now)
    }

    @Test
    fun `refreshWatchdog - Shall fail the UnitOfWork if the watchdog owner does not match`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.IN_PROGRESS.dbOrdinal,
            nextScheduledFor = null,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItems(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow

        dao.refreshWatchdog(
            queueId = insertedItem.queueId.toWorkItemQueueId(),
            itemId = insertedItem.itemId.toWorkItemId(),
            watchdogId = "wrong-watchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        assertThatThrownBy {
            runBlocking {
                unitOfWork.commit()
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `rescheduleAllFailedItems - Shall reschedule all failed items`(): Unit = runBlocking {
        val queueId = "testQueueId".toWorkItemQueueId()

        debugInsertItems(
            makeWorkItem("1", state = WorkItemState.FAILED.dbOrdinal),
            makeWorkItem("2", state = WorkItemState.FAILED.dbOrdinal),
            makeWorkItem("3", state = WorkItemState.SUCCESSFUL.dbOrdinal),
        )

        val now = testClock.dbNow

        dao.rescheduleAllFailedItems(queueId, now)

        val item1 = dao.getById(queueId, "1".toWorkItemId())!!
        val item2 = dao.getById(queueId, "2".toWorkItemId())!!
        val item3 = dao.getById(queueId, "3".toWorkItemId())!!

        assertThat(item1.state).isEqualTo(WorkItemState.SCHEDULED.dbOrdinal)
        assertThat(item1.nextScheduledFor).isEqualTo(now)

        assertThat(item2.state).isEqualTo(WorkItemState.SCHEDULED.dbOrdinal)
        assertThat(item2.nextScheduledFor).isEqualTo(now)

        assertThat(item3.state).isEqualTo(WorkItemState.SUCCESSFUL.dbOrdinal)
    }

    @Test
    fun `rescheduleAllFailedItems - Shall record state transition events`(): Unit = runBlocking {
        // Arrange
        val insertedItem = makeWorkItem("1", state = WorkItemState.FAILED.dbOrdinal)
        debugInsertItems(insertedItem)

        val now = testClock.dbNow

        // Act
        dao.rescheduleAllFailedItems(insertedItem.queueId.toWorkItemQueueId(), now)

        // Assert
        assertTransitionEvents(
            insertedItem,
            { it is WorkItemEvent.ManuallyRescheduled && it.timestamp == now && it.scheduledFor == now }
        )
    }

    @Test
    fun `rescheduleAllHangedItems - Shall reschedule all hanged items`(): Unit = runBlocking {
        val queueId = "testQueueId".toWorkItemQueueId()
        val testWatchdogTimeoutSeconds = 30L

        // Arrange
        val now = testClock.dbNow

        debugInsertItems(
            makeWorkItem(
                "1",
                state = WorkItemState.IN_PROGRESS.dbOrdinal,
                watchdogTimestamp = now.minusSeconds(testWatchdogTimeoutSeconds)
            ),
            makeWorkItem(
                "2",
                state = WorkItemState.IN_PROGRESS.dbOrdinal,
                watchdogTimestamp = now.minusSeconds(testWatchdogTimeoutSeconds - 2)
            ),
            makeWorkItem(
                "3",
                state = WorkItemState.SUCCESSFUL.dbOrdinal,
                watchdogTimestamp = null
            ),
            makeWorkItem(
                "4",
                state = WorkItemState.FAILED.dbOrdinal,
                watchdogTimestamp = null
            )
        )

        // Act
        dao.rescheduleAllHangedItems(
            rescheduleIfWatchdogOlderThan = now.minusSeconds(testWatchdogTimeoutSeconds - 1),
            scheduleFor = now,
            now = now
        )

        // Assert
        val item1 = dao.getById(queueId, "1".toWorkItemId())!!
        val item2 = dao.getById(queueId, "2".toWorkItemId())!!
        val item3 = dao.getById(queueId, "3".toWorkItemId())!!
        val item4 = dao.getById(queueId, "4".toWorkItemId())!!

        assertThat(item1.state).isEqualTo(WorkItemState.SCHEDULED.dbOrdinal)
        assertThat(item1.nextScheduledFor).isEqualTo(now)

        assertThat(item2.state).isEqualTo(WorkItemState.IN_PROGRESS.dbOrdinal)

        assertThat(item3.state).isEqualTo(WorkItemState.SUCCESSFUL.dbOrdinal)

        assertThat(item4.state).isEqualTo(WorkItemState.FAILED.dbOrdinal)
    }

    @Test
    fun `rescheduleAllHangedItems - Shall record state transition events`(): Unit = runBlocking {
        val testWatchdogTimeoutSeconds = 30L

        // Arrange
        val now = testClock.dbNow

        val insertedItem = makeWorkItem(
            "1",
            state = WorkItemState.IN_PROGRESS.dbOrdinal,
            watchdogTimestamp = now.minusSeconds(testWatchdogTimeoutSeconds)
        )
        debugInsertItems(insertedItem)

        // Act
        dao.rescheduleAllHangedItems(
            rescheduleIfWatchdogOlderThan = now.minusSeconds(testWatchdogTimeoutSeconds - 1),
            scheduleFor = now,
            now = now
        )

        // Assert
        assertTransitionEvents(
            insertedItem,
            { it is WorkItemEvent.Hanged && it.timestamp == now },
            { it is WorkItemEvent.AutomaticallyRescheduled && it.timestamp == now && it.scheduledFor == now }
        )
    }

    @Test
    fun `deleteAllFailedItems - Shall delete all failed items`(): Unit = runBlocking {
        // Arrange
        val queueId = "testQueueId".toWorkItemQueueId()

        debugInsertItems(
            makeWorkItem("1", state = WorkItemState.FAILED.dbOrdinal),
            makeWorkItem("2", state = WorkItemState.FAILED.dbOrdinal),
            makeWorkItem("3", state = WorkItemState.SUCCESSFUL.dbOrdinal),
            makeWorkItem("4", state = WorkItemState.IN_PROGRESS.dbOrdinal),
            makeWorkItem("5", state = WorkItemState.SCHEDULED.dbOrdinal),
            makeWorkItem("6", state = WorkItemState.FAILED.dbOrdinal),
        )

        // Act
        dao.deleteAllFailedItems(queueId)

        // Assert
        assertThat(dao.getById(queueId, "1".toWorkItemId())).isNull()
        assertThat(dao.getById(queueId, "2".toWorkItemId())).isNull()
        assertThat(dao.getById(queueId, "3".toWorkItemId())).isNotNull()
        assertThat(dao.getById(queueId, "4".toWorkItemId())).isNotNull()
        assertThat(dao.getById(queueId, "5".toWorkItemId())).isNotNull()
        assertThat(dao.getById(queueId, "6".toWorkItemId())).isNull()
    }

    @Test
    fun `deleteExpiredItems - Shall delete all items with an expiration time equal to or less than now`(): Unit =
        runBlocking {
            debugInsertItems(makeWorkItem("1", expiresAt = Instant.parse("2025-03-02T09:59:59Z")))
            debugInsertItems(makeWorkItem("2", expiresAt = Instant.parse("2025-03-02T10:00:00Z")))
            debugInsertItems(makeWorkItem("3", expiresAt = Instant.parse("2025-03-02T10:00:01Z")))

            testClock.reset(Instant.parse("2025-03-02T10:00:00Z"))

            val numDeleted = dao.deleteExpiredItems(testClock.dbNow)

            assertThat(numDeleted).isEqualTo(2)

            val allItems = dao.debugListAllItems().items
            assertThat(allItems).hasSize(1)
            assertThat(allItems[0].itemId).isEqualTo("3")
        }

    @Test
    fun `deleteFailedItem - Shall only delete a failed item`(): Unit = runBlocking {
        // Arrange
        val queueId = "testQueueId".toWorkItemQueueId()

        debugInsertItems(
            makeWorkItem("1", state = WorkItemState.FAILED.dbOrdinal),
            makeWorkItem("2", state = WorkItemState.SUCCESSFUL.dbOrdinal),
            makeWorkItem("3", state = WorkItemState.IN_PROGRESS.dbOrdinal),
            makeWorkItem("4", state = WorkItemState.SCHEDULED.dbOrdinal),
        )

        assertThat(dao.deleteFailedItem(queueId, "1".toWorkItemId())).isTrue()
        assertThat(dao.deleteFailedItem(queueId, "2".toWorkItemId())).isFalse()
        assertThat(dao.deleteFailedItem(queueId, "3".toWorkItemId())).isFalse()
        assertThat(dao.deleteFailedItem(queueId, "4".toWorkItemId())).isFalse()

        val allItemIds = dao.debugListAllItems().items.map { it.itemId }.toSet()
        assertThat(allItemIds).isEqualTo(setOf("2", "3", "4"))
    }

    private suspend fun debugInsertItems(vararg items: WorkItemDbEntity) {
        ufw.database.unitOfWorkFactory.use {
            for (item in items) {
                dao.debugInsert(item, unitOfWork = it)
            }
        }
    }

    private fun makeWorkItem(
        itemId: String = "testId",
        queueId: String = "testQueueId",
        state: Int = WorkItemState.SCHEDULED.dbOrdinal,
        concurrencyKey: String? = null,
        createdAt: Instant = testClock.instant(),
        firstScheduledFor: Instant = testClock.instant(),
        nextScheduledFor: Instant? = testClock.instant(),
        stateChangedAt: Instant = testClock.instant(),
        watchdogTimestamp: Instant? = null,
        watchdogOwner: String? = null,
        numFailures: Int = 0,
        expiresAt: Instant? = null,
    ): WorkItemDbEntity {
        return WorkItemDbEntity(
            uid = 0L,
            itemId = itemId,
            queueId = queueId,
            type = "testType",
            state = state,
            dataJson = """{}""",
            metadataJson = """{}""",
            concurrencyKey = concurrencyKey,
            createdAt = createdAt,
            firstScheduledFor = firstScheduledFor,
            nextScheduledFor = nextScheduledFor,
            stateChangedAt = stateChangedAt,
            watchdogTimestamp = watchdogTimestamp,
            watchdogOwner = watchdogOwner,
            numFailures = numFailures,
            expiresAt = expiresAt,
        )
    }

    private suspend fun dumpEvents(queueId: WorkItemQueueId, itemId: WorkItemId) {
        val events = dao.getEventsForItem(queueId, itemId)
        events.forEach { println(it) }
    }

    private suspend fun assertTransitionEvents(
        queueId: WorkItemQueueId,
        itemId: WorkItemId,
        vararg expected: (WorkItemEvent) -> Boolean
    ) {
        val events = dao.getEventsForItem(queueId, itemId)

        var i = 0
        for (event in events) {
            val expectedPredicate = expected[i]
            assertThat(expectedPredicate(event)).isTrue()

            i++
        }
    }

    private suspend fun assertTransitionEvents(item: WorkItemDbEntity, vararg expected: (WorkItemEvent) -> Boolean) {
        return assertTransitionEvents(item.queueId.toWorkItemQueueId(), item.itemId.toWorkItemId(), *expected)
    }
}
