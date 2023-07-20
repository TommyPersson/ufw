package io.tpersson.ufw.transactionalevents.handler.internal

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeName
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.test.TestInstantSource
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.dsl.transactionalEvents
import io.tpersson.ufw.transactionalevents.handler.EventContext
import io.tpersson.ufw.transactionalevents.handler.EventHandler
import io.tpersson.ufw.transactionalevents.handler.EventState
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Instant

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
            }
        }

        val testEventHandler1 = TestEventHandler1(ufw.keyValueStore.keyValueStore)

        val keyValueStore = ufw.keyValueStore.keyValueStore
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory
        val publisher = ufw.transactionalEvents.eventPublisher
        val eventQueueDAO = ufw.transactionalEvents.eventQueueDAO

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

    private suspend fun waitUntilQueueIsCompleted() {
        await.untilCallTo {
            runBlocking { eventQueueDAO.debugGetAllEvents() }
        } matches {
            it!!.size > 0 && it.all { job -> job.state in setOf(EventState.Successful.id, EventState.Failed.id) }
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
        override val id: EventId = EventId(),
        override val timestamp: Instant = Instant.now()
    ) : Event {
        @JsonIgnore
        val resultKey = KeyValueStore.Key.of<String>("test-results:TestEvent1:$id")
    }

    class TestEventHandler1(
        private val keyValueStore: KeyValueStore,
    ) : TransactionalEventHandler() {

        @EventHandler(topic = "test-topic")
        suspend fun handle(event: TestEvent1, context: EventContext) {
            if (event.shouldFail) {
                error("Failed")
            }

            keyValueStore.put(event.resultKey, event.text, unitOfWork = context.unitOfWork)
        }
    }
}