package io.tpersson.ufw.durablemessages.publisher.internal

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.configuration.entry
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablemessages.component.installDurableMessages
import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.MessageDefinition
import io.tpersson.ufw.durablemessages.component.durableMessages
import io.tpersson.ufw.durablemessages.configuration.DurableMessages
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import io.tpersson.ufw.managed.component.managed
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

        val testClock = TestClock()
        val testOutgoingEventTransport = TestOutgoingMessageTransport()

        val ufw = UFW.build {
            installCore {
                clock = testClock
                configProvider = ConfigProvider.fromEntries(
                    Configs.DurableMessages.OutboxWorkerEnabled.entry(true),
                )
            }
            installDatabase {
                dataSource = HikariDataSource(config)
            }
            installDurableMessages {
                outgoingMessageTransport = testOutgoingEventTransport
            }
        }

        val unitOfWorkFactory = ufw.database.unitOfWorkFactory

        val publisher = ufw.durableMessages.messagePublisher

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
        val event = TestMessage(
            id = DurableMessageId(),
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
        val event = TestMessage(
            id = DurableMessageId(),
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
        val event = TestMessage(
            id = DurableMessageId(),
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

    @MessageDefinition("TEST_EVENT", "test-topic")
    data class TestMessage(
        override val id: DurableMessageId,
        override val timestamp: Instant,
        val content: String,
    ) : DurableMessage()

    class TestOutgoingMessageTransport : OutgoingMessageTransport {
        var sentBatches: MutableList<List<OutgoingMessage>> = mutableListOf()

        override suspend fun send(messages: List<OutgoingMessage>, unitOfWork: UnitOfWork) {
            sentBatches.add(messages)
        }
    }
}