package io.tpersson.ufw.databasequeue

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.exceptions.MinimumAffectedRowsException
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemEvent
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.test.TestInstantSource
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

        val testClock = TestInstantSource()

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
                queueId = "testQueueId",
                itemId = "testId",
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

        val item = dao.getById(queueId = "testQueueId", itemId = "testId")!!

        assertThat(item.uid).isGreaterThan(0)
        assertThat(item.itemId).isEqualTo("testId")
        assertThat(item.queueId).isEqualTo("testQueueId")
        assertThat(item.type).isEqualTo("testType")
        assertThat(item.dataJson).isEqualTo("dataJson")
        assertThat(item.state).isEqualTo(WorkItemState.SCHEDULED)
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
                queueId = "testQueueId",
                itemId = "testId",
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

        val item = dao.getById(queueId = "testQueueId", itemId = "testId")!!

        assertTransitionEvents(
            item,
            { it is WorkItemEvent.Scheduled && it.timestamp == now && it.scheduledFor == scheduleFor }
        )
    }

    @Test
    fun `takeNext - Takes a SCHEDULED item and moves it to IN_PROGRESS`(): Unit = runBlocking {
        debugInsertItem(makeWorkItem(itemId = "testId", queueId = "testQueueId"))
        testClock.advance(Duration.ofMinutes(2))

        val now = testClock.instant().truncatedTo(ChronoUnit.MILLIS)

        run {
            val item = dao.takeNext(queueId = "testQueueId", watchdogId = "testWatchdog", now = now)

            assertThat(item?.state).isEqualTo(WorkItemState.IN_PROGRESS)
            assertThat(item?.stateChangedAt).isEqualTo(now)
            assertThat(item?.watchdogOwner).isEqualTo("testWatchdog")
            assertThat(item?.watchdogTimestamp).isEqualTo(now)
        }

        run {
            val item = dao.listAllItems().first { it.itemId == "testId" }

            assertThat(item.state).isEqualTo(WorkItemState.IN_PROGRESS)
            assertThat(item.stateChangedAt).isEqualTo(now)
            assertThat(item.watchdogOwner).isEqualTo("testWatchdog")
            assertThat(item.watchdogTimestamp).isEqualTo(now)
        }
    }

    @Test
    fun `takeNext - Shall not take an item with the same concurrency as an already IN_PROGRESS item`(): Unit =
        runBlocking {
            debugInsertItem(makeWorkItem(itemId = "testId1", concurrencyKey = "concurrencyKey"))
            testClock.advance(Duration.ofMinutes(2))
            debugInsertItem(makeWorkItem(itemId = "testId2", concurrencyKey = "concurrencyKey"))
            testClock.advance(Duration.ofMinutes(2))

            val now = testClock.instant().truncatedTo(ChronoUnit.MILLIS)

            run {
                val item = dao.takeNext(queueId = "testQueueId", watchdogId = "testWatchdog", now = now)

                assertThat(item?.itemId).isEqualTo("testId1")
            }

            run {
                val item = dao.takeNext(queueId = "testQueueId", watchdogId = "testWatchdog", now = now)

                assertThat(item).isNull()
            }
        }

    @Test
    fun `takeNext - Shall record state transition event`(): Unit = runBlocking {
        // Arrange
        debugInsertItem(makeWorkItem(itemId = "testId1", concurrencyKey = "concurrencyKey"))

        testClock.advance(Duration.ofMinutes(2))
        val now = testClock.dbNow

        // Act
        val item = dao.takeNext(queueId = "testQueueId", watchdogId = "testWatchdog", now = now)!!

        // Assert
        assertTransitionEvents(
            queueId = item.queueId,
            itemId = item.itemId,
            { it is WorkItemEvent.Taken && it.timestamp == now },
        )
    }

    @Test
    fun `markInProgressItemAsSuccessful - Shall record state transition events`(): Unit = runBlocking {
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = item.queueId,
            itemId = item.itemId,
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
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = item.queueId,
            itemId = item.itemId,
            expiresAt = expiresAt,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.listAllItems().first { it.itemId == "testId1" }

        assertThat(item2.state).isEqualTo(WorkItemState.SUCCESSFUL)
        assertThat(item2.watchdogOwner).isNull()
        assertThat(item2.watchdogTimestamp).isNull()
        assertThat(item2.expiresAt).isEqualTo(expiresAt)
        assertThat(item2.stateChangedAt).isEqualTo(now)
        assertThat(item2.nextScheduledFor).isNull()

        dumpEvents(item2.queueId, item2.itemId)
    }

    @Test
    fun `markInProgressItemAsSuccessful - Shall fail UnitOfWork if the watchdog is incorrect`(): Unit = runBlocking {
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = item.queueId,
            itemId = item.itemId,
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
        debugInsertItem(makeWorkItem(itemId = "testId1", queueId = "testQueueId", watchdogOwner = "watchdog"))
        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsSuccessful(
            queueId = "testId1",
            itemId = "testQueueId",
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
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = item.queueId,
            itemId = item.itemId,
            expiresAt = expiresAt,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.listAllItems().first { it.itemId == "testId1" }

        assertThat(item2.state).isEqualTo(WorkItemState.FAILED)
        assertThat(item2.watchdogOwner).isNull()
        assertThat(item2.watchdogTimestamp).isNull()
        assertThat(item2.expiresAt).isEqualTo(expiresAt)
        assertThat(item2.stateChangedAt).isEqualTo(now)
        assertThat(item2.nextScheduledFor).isNull()
    }

    @Test
    fun `markInProgressItemAsFailed - Shall record state transition events`(): Unit = runBlocking {
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = item.queueId,
            itemId = item.itemId,
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
    fun `markInProgressItemAsFailed - Shall fail UnitOfWork if the watchdog is incorrect`(): Unit = runBlocking {
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = item.queueId,
            itemId = item.itemId,
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
        debugInsertItem(makeWorkItem(itemId = "testId1", queueId = "testQueueId", watchdogOwner = "watchdog"))
        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expiresAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.markInProgressItemAsFailed(
            queueId = "testId1",
            itemId = "testQueueId",
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
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.rescheduleInProgressItem(
            queueId = item.queueId,
            itemId = item.itemId,
            scheduleFor = scheduleFor,
            watchdogId = "testWatchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item2 = dao.listAllItems().first { it.itemId == "testId1" }

        assertThat(item2.state).isEqualTo(WorkItemState.SCHEDULED)
        assertThat(item2.watchdogOwner).isNull()
        assertThat(item2.watchdogTimestamp).isNull()
        assertThat(item2.expiresAt).isNull()
        assertThat(item2.stateChangedAt).isEqualTo(now)
        assertThat(item2.nextScheduledFor).isEqualTo(scheduleFor)

        dumpEvents(item2.queueId, item2.itemId)
    }


    @Test
    fun `rescheduleInProgressItem - Shall record state transition events`(): Unit = runBlocking {
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        unitOfWorkFactory.use { uow ->
            dao.rescheduleInProgressItem(
                queueId = item.queueId,
                itemId = item.itemId,
                scheduleFor = scheduleFor,
                watchdogId = "testWatchdog",
                now = now,
                unitOfWork = uow
            )
        }

        assertTransitionEvents(
            queueId = item.queueId,
            itemId = item.itemId,
            { it is WorkItemEvent.Taken },
            { it is WorkItemEvent.Failed && it.timestamp == now },
            { it is WorkItemEvent.AutomaticallyRescheduled && it.timestamp == now && it.scheduledFor == scheduleFor }
        )
    }

    @Test
    fun `rescheduleInProgressItem - Shall fail UnitOfWork if the watchdog is incorrect`(): Unit = runBlocking {
        debugInsertItem(makeWorkItem(itemId = "testId1"))
        testClock.advance(Duration.ofMinutes(2))

        val item = dao.takeNext(
            queueId = "testQueueId",
            watchdogId = "testWatchdog",
            now = testClock.dbNow
        )!!

        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.rescheduleInProgressItem(
            queueId = item.queueId,
            itemId = item.itemId,
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
        debugInsertItem(makeWorkItem(itemId = "testId1", queueId = "testQueueId", watchdogOwner = "watchdog"))
        testClock.advance(Duration.ofMinutes(2))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.rescheduleInProgressItem(
            queueId = "testId1",
            itemId = "testQueueId",
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
            state = WorkItemState.FAILED,
            nextScheduledFor = null,
        )

        debugInsertItem(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.manuallyRescheduleFailedItem(
            queueId = insertedItem.queueId,
            itemId = insertedItem.itemId,
            scheduleFor = scheduleFor,
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById(queueId = insertedItem.queueId, itemId = insertedItem.itemId)!!

        assertThat(item.state).isEqualTo(WorkItemState.SCHEDULED)
        assertThat(item.stateChangedAt).isEqualTo(now)
        assertThat(item.nextScheduledFor).isEqualTo(scheduleFor)
        assertThat(item.expiresAt).isNull()
    }

    @Test
    fun `manuallyRescheduleFailedItem - Shall record state transition events`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.FAILED,
            nextScheduledFor = null,
        )

        debugInsertItem(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.manuallyRescheduleFailedItem(
            queueId = insertedItem.queueId,
            itemId = insertedItem.itemId,
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

        debugInsertItem(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val scheduleFor = testClock.dbNow.plus(Duration.ofDays(1))

        dao.manuallyRescheduleFailedItem(
            queueId = insertedItem.queueId,
            itemId = insertedItem.itemId,
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
            state = WorkItemState.IN_PROGRESS,
            nextScheduledFor = null,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItem(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expireAt = testClock.dbNow.plus(Duration.ofDays(1))

        dao.forceCancelItem(
            queueId = insertedItem.queueId,
            itemId = insertedItem.itemId,
            now = now,
            expireAt = expireAt,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById(queueId = insertedItem.queueId, itemId = insertedItem.itemId)!!

        assertThat(item.state).isEqualTo(WorkItemState.CANCELLED)
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
            state = WorkItemState.IN_PROGRESS,
            nextScheduledFor = testClock.dbNow,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItem(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow
        val expireAt = testClock.dbNow.plus(Duration.ofDays(1))

        // Act
        dao.forceCancelItem(
            queueId = insertedItem.queueId,
            itemId = insertedItem.itemId,
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
            state = WorkItemState.IN_PROGRESS,
            nextScheduledFor = null,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItem(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow

        dao.refreshWatchdog(
            queueId = insertedItem.queueId,
            itemId = insertedItem.itemId,
            watchdogId = "watchdog",
            now = now,
            unitOfWork = unitOfWork
        )

        unitOfWork.commit()

        val item = dao.getById(queueId = insertedItem.queueId, itemId = insertedItem.itemId)!!

        assertThat(item.watchdogOwner).isEqualTo("watchdog")
        assertThat(item.watchdogTimestamp).isEqualTo(now)
    }

    @Test
    fun `refreshWatchdog - Shall fail the UnitOfWork if the watchdog owner does not match`(): Unit = runBlocking {
        val insertedItem = makeWorkItem(
            itemId = "testId",
            queueId = "testQueueId",
            state = WorkItemState.IN_PROGRESS,
            nextScheduledFor = null,
            watchdogOwner = "watchdog",
            watchdogTimestamp = testClock.dbNow
        )

        debugInsertItem(insertedItem)

        testClock.advance(Duration.ofDays(1))

        val unitOfWork = unitOfWorkFactory.create()

        val now = testClock.dbNow

        dao.refreshWatchdog(
            queueId = insertedItem.queueId,
            itemId = insertedItem.itemId,
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

    private suspend fun debugInsertItem(item: WorkItemDbEntity) {
        ufw.database.unitOfWorkFactory.use {
            dao.debugInsert(item, unitOfWork = it)
        }
    }

    private fun makeWorkItem(
        itemId: String = "testId",
        queueId: String = "testQueueId",
        state: Int = WorkItemState.SCHEDULED,
        concurrencyKey: String? = null,
        createdAt: Instant = testClock.instant(),
        firstScheduledFor: Instant = testClock.instant(),
        nextScheduledFor: Instant? = testClock.instant(),
        stateChangedAt: Instant = testClock.instant(),
        watchdogTimestamp: Instant? = null,
        watchdogOwner: String? = null,
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
            expiresAt = expiresAt,
        )
    }

    private suspend fun dumpEvents(queueId: String, itemId: String) {
        val events = dao.getEventsForItem(queueId, itemId)
        events.forEach { println(it) }
    }

    private suspend fun assertTransitionEvents(
        queueId: String,
        itemId: String,
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
        return assertTransitionEvents(item.queueId, item.itemId, *expected)
    }
}