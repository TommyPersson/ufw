@file:Suppress("DEPRECATION")

package io.tpersson.ufw.adapters.durablemessages.kafka

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.adapters.durablemessages.kafka.component.installDurableMessagesKafka
import io.tpersson.ufw.adapters.durablemessages.kafka.configuration.DurableMessagesKafka
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.configuration.entry
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.MessageDefinition
import io.tpersson.ufw.durablemessages.component.durableMessages
import io.tpersson.ufw.durablemessages.configuration.DurableMessages
import io.tpersson.ufw.durablemessages.handler.DurableMessageContext
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import io.tpersson.ufw.durablemessages.handler.annotations.MessageHandler
import io.tpersson.ufw.managed.component.managed
import io.tpersson.ufw.test.TestClock
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant

internal class DurableMessagesKafkaIntegrationTests {

    private companion object {
        @JvmStatic
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15")).also {
            Startables.deepStart(it).join()
        }

        @JvmStatic
        var kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).withKraft().also {
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
            installCore {
                clock = testClock
                configProvider = ConfigProvider.fromEntries(
                    Configs.DurableMessages.OutboxWorkerEnabled.entry(true),
                    Configs.DurableMessages.OutboxWorkerInterval.entry(Duration.ofMillis(5)),
                    Configs.DurableMessagesKafka.ConsumerEnabled.entry(true),
                    Configs.DurableMessagesKafka.ConsumerPollWaitTime.entry(Duration.ofMillis(5)),
                    Configs.DurableMessagesKafka.Consumer.entry(
                        mapOf(
                            "bootstrap.servers" to kafka.bootstrapServers,
                            "auto.offset.reset" to "earliest", // The consumer could start after publishes
                        )
                    ),
                    Configs.DurableMessagesKafka.Producer.entry(
                        mapOf(
                            "bootstrap.servers" to kafka.bootstrapServers,
                        )
                    )
                )
            }
            installDatabase {
                dataSource = HikariDataSource(config)
            }
            installDurableMessagesKafka()
        }


        val unitOfWorkFactory = ufw.database.unitOfWorkFactory
        val messagePublisher = ufw.durableMessages.messagePublisher

        init {
            ufw.durableMessages.register(TestMessageHandler())

            ufw.database.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        TestMessageHandler.handledMessage = null

        ufw.managed.startAll()
    }

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        ufw.managed.stopAll()
    }

    @Test
    fun `Should be able to receive published messages`(): Unit = runBlocking {
        unitOfWorkFactory.use { uow ->
            messagePublisher.publish(
                message = TestMessage("Hello, World!"),
                unitOfWork = uow,
            )
        }

        await.until {
            TestMessageHandler.handledMessage?.message == "Hello, World!"
        }
    }

    @MessageDefinition(
        type = "test-event-v1",
        topic = "test-topic",
    )
    data class TestMessage(
        val message: String,
        override val id: DurableMessageId = DurableMessageId(),
        override val timestamp: Instant = Instant.now(),
    ) : DurableMessage()

    public class TestMessageHandler @Inject constructor() : DurableMessageHandler {

        companion object {
            var handledMessage: TestMessage? = null
        }

        @MessageHandler(topic = "test-topic")
        public suspend fun handle(message: TestMessage, context: DurableMessageContext): Unit {
            context.unitOfWork.addPostCommitHook { handledMessage = message }
        }
    }
}