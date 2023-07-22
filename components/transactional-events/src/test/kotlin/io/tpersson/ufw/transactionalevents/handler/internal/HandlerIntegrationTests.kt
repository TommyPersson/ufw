package io.tpersson.ufw.transactionalevents.handler.internal

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeName
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.test.TestInstantSource
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.dsl.transactionalEvents
import io.tpersson.ufw.transactionalevents.handler.*
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventQueueDAOImpl
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@Timeout(5)
internal class HandlerIntegrationTests {

    private companion object {
        @JvmStatic
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15")).also {
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
            managed {
            }
            database {
                dataSource = HikariDataSource(config)
            }
            keyValueStore {
            }
            transactionalEvents {
                configure {
                    stalenessDetectionInterval = Duration.ofMillis(100)
                    stalenessAge = Duration.ofMillis(500)
                    queuePollWaitTime = Duration.ofMillis(20)
                    watchdogRefreshInterval = Duration.ofMillis(25)
                    successfulEventRetention = Duration.ofDays(1)
                    failedEventRetention = Duration.ofDays(2)
                    expiredEventReapingInterval = Duration.ofMillis(50)
                }
            }
        }

        val testEventHandler1 = TestEventHandler1(ufw.keyValueStore.keyValueStore, testClock)

        val keyValueStore = ufw.keyValueStore.keyValueStore
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory
        val publisher = ufw.transactionalEvents.eventPublisher
        val eventQueueDAO = ufw.transactionalEvents.eventQueueDAO
        val eventFailuresDAO = ufw.transactionalEvents.eventFailuresDAO
        val staleEventsRescheduler = ufw.transactionalEvents.staleEventRescheduler
        val database = ufw.database.database

        init {
            ufw.transactionalEvents.registerHandler(testEventHandler1)
            ufw.database.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        ufw.managed.managedRunner.startAll()
    }

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        ufw.managed.managedRunner.stopAll()
        eventQueueDAO.debugTruncate()
    }

    @Test
    fun `Basic - Can handle events`(): Unit = runBlocking {
        val event = TestEvent1("Hello, World!")

        publish("test-topic", event)

        waitUntilQueueIsCompleted()

        assertThat(keyValueStore.get(event.resultKey)?.value).isEqualTo(event.text)
    }

    @Test
    fun `Basic - Enqueueing the same event ID to the same queue is idempotent`(): Unit = runBlocking {
        val event = TestEvent1("Hello, World!")
        val eventDuplicate = event.copy()

        publish("test-topic", event)
        publish("test-topic", eventDuplicate)

        waitUntilQueueIsCompleted()

        val allEvents = eventQueueDAO.debugGetAllEvents(testEventHandler1.eventQueueId)

        assertThat(allEvents).hasSize(1)
    }

    @Test
    fun `Failures - Event state is set to 'Failed' on failure`(): Unit = runBlocking {
        val testEvent = TestEvent1("Hello, World!", shouldFail = true)

        publish("test-topic", testEvent)

        waitUntilQueueIsCompleted()

        val event = eventQueueDAO.getById(testEventHandler1.eventQueueId, testEvent.id)!!
        assertThat(event.state).isEqualTo(EventState.Failed.id)
    }

    @Test
    fun `Failures - An EventFailure is recorded for each failure`(): Unit = runBlocking {
        val testEvent = TestEvent1("Hello, World!", shouldFail = true, numRetries = 3)

        publish("test-topic", testEvent)

        waitUntilQueueIsCompleted()

        val event = eventQueueDAO.getById(testEventHandler1.eventQueueId, testEvent.id)!!
        val numFailures = eventFailuresDAO.getNumberOfFailuresFor(event.uid!!)
        val failures = eventFailuresDAO.getLatestFor(event.uid!!, limit = 10)

        assertThat(numFailures).isEqualTo(4)
        assertThat(failures).hasSize(4)
    }

    @Test
    fun `Failures - JSON deserialization errors are recorded as failures`(): Unit = runBlocking {
        val eventId = EventId()

        database.update(
            EventQueueDAOImpl.Queries.Updates.Insert(
                EventEntityData(
                    uid = 0,
                    id = eventId.value,
                    queueId = testEventHandler1.eventQueueId.id,
                    topic = "test-topic",
                    type = "TestEvent1",
                    state = EventState.Scheduled.id,
                    dataJson = """{ "hasMissing": "properties" }""",
                    ceDataJson = "{}",
                    createdAt = testClock.instant(),
                    scheduledFor = testClock.instant(),
                    stateChangedAt = testClock.instant(),
                    expireAt = null,
                    watchdogTimestamp = null,
                    watchdogOwner = null,
                    timestamp = testClock.instant()
                )
            )
        )

        waitUntilQueueIsCompleted()

        val event = eventQueueDAO.getById(testEventHandler1.eventQueueId, eventId)!!
        val numFailures = eventFailuresDAO.getNumberOfFailuresFor(event.uid!!)
        val failures = eventFailuresDAO.getLatestFor(event.uid!!, limit = 10)

        assertThat(numFailures).isEqualTo(1)
        assertThat(failures).hasSize(1)
    }

    @Test
    fun `Staleness - Stale events are automatically rescheduled`(): Unit = runBlocking {
        val eventId = EventId(UUID.randomUUID().toString())
        val queueId = testEventHandler1.eventQueueId

        database.update(
            EventQueueDAOImpl.Queries.Updates.Insert(
                EventEntityData(
                    uid = 0,
                    id = eventId.value,
                    queueId = queueId.id,
                    topic = "test-topic",
                    type = "TestEvent1",
                    state = EventState.InProgress.id,
                    dataJson = """
                            {
                                "id": "$eventId",
                                "queueId": "$queueId",
                                "@type": "TestEvent1",
                                "text": "hello",
                                "numFailures": 0,
                                "numRetries": 0,
                                "timestamp": "2022-02-02T22:22:22Z"
                            }
                        """.trimIndent(),
                    ceDataJson = "{}",
                    createdAt = testClock.instant(),
                    scheduledFor = testClock.instant(),
                    stateChangedAt = testClock.instant(),
                    expireAt = null,
                    watchdogTimestamp = testClock.instant().minus(Duration.ofSeconds(1)),
                    watchdogOwner = "anyone",
                    timestamp = testClock.instant()
                )
            )
        )

        // Then wait for the job to complete normally
        waitUntilQueueIsCompleted()

        val job = eventQueueDAO.getById(queueId, eventId)!!

        assertThat(job.state).isEqualTo(EventState.Successful.id)
        assertThat(staleEventsRescheduler.hasFoundStaleEvents).isTrue()
    }


    @Test
    fun `Staleness - An automatic watchdog refresher keeps long-running jobs from being stale`(): Unit = runBlocking {
        val testEvent = TestEvent1("Hello, World!", delayTime = Duration.ofSeconds(1))

        publish("test-topic", testEvent)

        await.pollDelay(Duration.ofMillis(1)).untilAssertedSuspend {
            assertThat(eventQueueDAO.getById(testEventHandler1.eventQueueId, testEvent.id)?.state)
                .isEqualTo(EventState.InProgress.id)
        }

        waitUntilQueueIsCompleted()

        val event = eventQueueDAO.getById(testEventHandler1.eventQueueId, testEvent.id)!!

        assertThat(event.state).isEqualTo(EventState.Successful.id)
        assertThat(staleEventsRescheduler.hasFoundStaleEvents).isFalse()
    }

    @Test
    fun `Staleness - If a runner loses ownership of a job, it will not modify its state or record failures`(): Unit =
        runBlocking {
            staleEventsRescheduler.stop()

            val testEvent = TestEvent1("Hello, World!", delayTime = Duration.ofMillis(200))

            publish("test-topic", testEvent)

            await.pollDelay(Duration.ofMillis(1)).untilAssertedSuspend {
                assertThat(eventQueueDAO.getById(testEventHandler1.eventQueueId, testEvent.id)?.state)
                    .isEqualTo(EventState.InProgress.id)
            }

            do {
                delay(1)
                val numStolen = database.update(TestQueries.Updates.StealAnyInProgressEvents)
            } while (numStolen == 0)

            // Testing "stuff not happening" is not fun. Add monitoring events to runner?
            delay(testEvent.delayTime!!.multipliedBy(2).toMillis())

            val event = eventQueueDAO.getById(testEventHandler1.eventQueueId, testEvent.id)!!

            assertThat(event.state).isEqualTo(EventState.InProgress.id)
            assertThat(eventFailuresDAO.getNumberOfFailuresFor(event.uid!!)).isZero()
        }

    @Test
    fun `Retention - Successful jobs are automatically removed after their retention duration`(): Unit = runBlocking {
        val testEvent = TestEvent1("Hello, World!")

        publish("test-topic", testEvent)

        waitUntilQueueIsCompleted()

        testClock.advance(Duration.ofDays(1).plusSeconds(1))

        await.untilAssertedSuspend {
            assertThat(eventQueueDAO.debugGetAllEvents()).isEmpty()
        }
    }

    @Test
    fun `Retention - Failed jobs are automatically removed after their retention duration`(): Unit = runBlocking {
        val testEvent = TestEvent1("Hello, World!", shouldFail = true)

        publish("test-topic", testEvent)

        waitUntilQueueIsCompleted()

        testClock.advance(Duration.ofDays(2).plusSeconds(1))

        await.untilAssertedSuspend {
            assertThat(eventQueueDAO.debugGetAllEvents()).isEmpty()
        }
    }

    private suspend fun waitUntilQueueIsCompleted() {
        val pollDelay = Duration.ofMillis(10)

        await.pollDelay(pollDelay).untilAssertedSuspend {
            testClock.advance(pollDelay)

            val allEvents = eventQueueDAO.debugGetAllEvents()
            assertThat(allEvents).isNotEmpty()
            assertThat(allEvents.map { it.state }).containsAnyOf(EventState.Successful.id, EventState.Failed.id)
        }
    }

    private suspend fun publish(topic: String, event: Event) {
        unitOfWorkFactory.use { uow ->
            publisher.publish(topic, event, uow)
        }
    }

    @JsonTypeName("TestEvent1")
    data class TestEvent1(
        val text: String,
        val shouldFail: Boolean = false,
        val numRetries: Int = 0,
        val delayTime: Duration? = null,
        override val id: EventId = EventId(),
        override val timestamp: Instant = Instant.now(),
    ) : Event {
        @JsonIgnore
        val resultKey = KeyValueStore.Key.of<String>("test-results:TestEvent1:$id")
    }

    class TestEventHandler1(
        private val keyValueStore: KeyValueStore,
        private val clock: TestInstantSource
    ) : TransactionalEventHandler() {

        @EventHandler(topic = "test-topic")
        suspend fun handle(event: TestEvent1, context: EventContext) = coroutineScope {
            if (event.shouldFail) {
                error("Failed")
            }

            if (event.delayTime != null) {
                delay(event.delayTime.toMillis())
            }

            keyValueStore.put(event.resultKey, event.text, unitOfWork = context.unitOfWork)
        }

        override fun onFailure(event: Event, error: Throwable, context: EventFailureContext): FailureAction {
            val numRetries = when (event) {
                is TestEvent1 -> event.numRetries
                else -> 0
            }

            return if (context.numberOfFailures > numRetries) {
                FailureAction.GiveUp
            } else {
                FailureAction.RescheduleNow
            }
        }
    }

    object TestQueries {
        object Updates {
            object StealAnyInProgressEvents : TypedUpdate(
                """
                UPDATE ufw__transactional_events__queue
                SET watchdog_owner = 'stolen'
                WHERE state = ${EventState.InProgress.id}
                """.trimIndent(),
                minimumAffectedRows = 0
            )
        }
    }
}

public fun org.awaitility.core.ConditionFactory.untilAssertedSuspend(block: suspend () -> Unit) {
    untilAsserted {
        runBlocking {
            block()
        }
    }
}