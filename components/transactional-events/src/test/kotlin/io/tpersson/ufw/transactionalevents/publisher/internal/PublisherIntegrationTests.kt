package io.tpersson.ufw.transactionalevents.publisher.internal

import com.fasterxml.jackson.annotation.JsonTypeName
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.test.TestInstantSource
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.EventDefinition
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.dsl.transactionalEvents
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEvent
import io.tpersson.ufw.transactionalevents.publisher.OutgoingEventTransport
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Instant

internal class PublisherIntegrationTests {

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
        val testOutgoingEventTransport = TestOutgoingEventTransport()

        val ufw = UFW.build {
            core {
                clock = testClock
            }
            managed {
            }
            database {
                dataSource = HikariDataSource(config)
            }
            transactionalEvents {
                outgoingEventTransport = testOutgoingEventTransport
            }
        }

        val unitOfWorkFactory = ufw.database.unitOfWorkFactory

        val publisher = ufw.transactionalEvents.eventPublisher

        init {
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
        testOutgoingEventTransport.sentBatches.clear()
    }

    @Test
    fun `Basic - Events shall be forwarded to the OutgoingEventTransport`(): Unit = runBlocking {
        val event = TestEvent(
            id = EventId(),
            timestamp = testClock.instant(),
            content = "Hello, World"
        )

        unitOfWorkFactory.use { uow ->
            publisher.publish(event, uow)
        }

        await.untilAsserted {
            assertThat(testOutgoingEventTransport.sentBatches.flatten()).anyMatch {
                it.id == event.id
            }
        }
    }

    @Test
    fun `Basic - Uncommitted events are not forwarded to the OutgoingEventTransport`(): Unit = runBlocking {
        val event = TestEvent(
            id = EventId(),
            timestamp = testClock.instant(),
            content = "Hello, World"
        )

        try {
            unitOfWorkFactory.use { uow ->
                publisher.publish(event, uow)
                error("error")
            }
        } catch (_: Exception) {
        }

        delay(100)

        assertThat(testOutgoingEventTransport.sentBatches).isEmpty()
    }


    @Test
    fun `Idempotency - Events with the same ID are not duplicated`(): Unit = runBlocking {
        val event = TestEvent(
            id = EventId(),
            timestamp = testClock.instant(),
            content = "Hello, World"
        )

        unitOfWorkFactory.use { uow ->
            publisher.publish(event, uow)
            publisher.publish(event.copy(), uow)
        }

        await.untilAsserted {
            assertThat(testOutgoingEventTransport.sentBatches.flatten()).hasSize(1)
        }
    }

    @EventDefinition("TEST_EVENT", "test-topic")
    data class TestEvent(
        override val id: EventId,
        override val timestamp: Instant,
        val content: String,
    ) : Event()

    class TestOutgoingEventTransport : OutgoingEventTransport {
        var sentBatches: MutableList<List<OutgoingEvent>> = mutableListOf()

        override suspend fun send(events: List<OutgoingEvent>, unitOfWork: UnitOfWork) {
            sentBatches.add(events)
        }
    }
}