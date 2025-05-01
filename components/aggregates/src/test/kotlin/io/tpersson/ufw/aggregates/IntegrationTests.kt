package io.tpersson.ufw.aggregates

import com.fasterxml.jackson.annotation.JsonTypeName
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.aggregates.component.AggregatesComponent
import io.tpersson.ufw.aggregates.component.aggregates
import io.tpersson.ufw.aggregates.component.installAggregates
import io.tpersson.ufw.aggregates.exceptions.AggregateVersionConflictException
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.MessageDefinition
import io.tpersson.ufw.durablemessages.common.messageDefinition
import io.tpersson.ufw.test.TestClock
import io.tpersson.ufw.durablemessages.component.installDurableMessages
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessage
import io.tpersson.ufw.durablemessages.publisher.OutgoingMessageTransport
import io.tpersson.ufw.managed.component.managed
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.*

internal class IntegrationTests {

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
                configProvider = ConfigProvider.fromText("""
                    [ufw.durable-messages]
                    outbox-worker-enabled = true
                """.trimIndent(), ConfigProvider.TextFormat.TOML)
            }
            installDatabase {
                dataSource = HikariDataSource(config)
            }
            installDurableMessages {
                outgoingMessageTransport = testOutgoingEventTransport
            }
            installAggregates()
        }

        val unitOfWorkFactory = ufw.database.unitOfWorkFactory
        val factRepository = ufw.aggregates.factRepository
        val repository = TestAggregateRepository(ufw.aggregates)

        init {
            ufw.database.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        ufw.managed.startAll()
    }

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        ufw.managed.stopAll()
        unitOfWorkFactory.use { uow ->
            factRepository.debugTruncate(uow)
        }
        testOutgoingEventTransport.sentMessages.clear()
    }

    @Test
    fun `Basic - Can modify and load aggregates`(): Unit = runBlocking {
        val now = testClock.instant()

        val new = TestAggregate.new()

        new.increment(now) // 1
        new.increment(now) // 2
        new.decrement(now) // 1

        unitOfWorkFactory.use { uow ->
            repository.save(new, uow)
        }

        val loaded1 = repository.getById(new.id)!!
        loaded1.increment(now) // 2

        unitOfWorkFactory.use { uow ->
            repository.save(loaded1, uow)
        }

        val loaded2 = repository.getById(new.id)!!

        assertThat(loaded2.counter).isEqualTo(2)
    }

    @Test
    fun `Basic - Can load Facts for aggregate`(): Unit = runBlocking {
        val now = testClock.instant()

        val new = TestAggregate.new()

        new.increment(now)
        new.decrement(now)

        unitOfWorkFactory.use { uow ->
            repository.save(new, uow)
        }

        val facts = factRepository.getAll(new.id, TestAggregate.Facts::class)

        assertThat(facts).hasSize(2)
        assertThat(facts[0]).isEqualTo(TestAggregate.Facts.Incremented(now))
        assertThat(facts[1]).isEqualTo(TestAggregate.Facts.Decremented(now))
    }

    @Test
    fun `Basic- Saving without new Facts is a no-op `(): Unit = runBlocking {
        val now = testClock.instant()

        val new = TestAggregate.new()

        new.increment(now)
        new.decrement(now)

        unitOfWorkFactory.use { uow ->
            repository.save(new, uow)
        }

        val loaded = repository.getById(new.id)!!

        unitOfWorkFactory.use { uow ->
            repository.save(loaded, uow)
        }

        assertThat(loaded.savedVersion).isNull()
    }

    @Test
    fun `Versioning - Version is incremented based on the amount of new Facts`(): Unit = runBlocking {
        val now = testClock.instant()

        val new = TestAggregate.new()

        new.increment(now)
        new.increment(now)

        unitOfWorkFactory.use { uow ->
            repository.save(new, uow)
        }

        val loaded = repository.getById(new.id)!!

        assertThat(new.savedVersion).isEqualTo(2)
        assertThat(loaded.originalVersion).isEqualTo(2)
    }

    @Test
    fun `Versioning - Concurrent modifications will fail`(): Unit = runBlocking {
        val now = testClock.instant()

        val new = TestAggregate.new()

        new.increment(now)

        unitOfWorkFactory.use { uow ->
            repository.save(new, uow)
        }

        val loaded1 = repository.getById(new.id)!!
        val loaded2 = repository.getById(new.id)!!

        loaded1.increment(now)
        loaded2.increment(now)

        unitOfWorkFactory.use { uow ->
            repository.save(loaded1, uow)
        }

        assertThatThrownBy {
            runBlocking {
                unitOfWorkFactory.use { uow ->
                    repository.save(loaded2, uow)
                }
            }
        }.matches {
            it is AggregateVersionConflictException && it.aggregateId == new.id
        }
    }

    @Test
    fun `Events - Are mapped from Facts and then published on save`(): Unit = runBlocking {
        val now = testClock.instant()

        val new = TestAggregate.new()

        new.increment(now)
        new.increment(now)
        new.decrement(now)

        unitOfWorkFactory.use { uow ->
            repository.save(new, uow)
        }

        await.untilAsserted {
            val sentEvents = testOutgoingEventTransport.sentMessages
            assertThat(sentEvents).hasSize(3)
            assertThat(sentEvents[0].type).isEqualTo(IncrementedMessage::class.messageDefinition.type)
            assertThat(sentEvents[1].type).isEqualTo(IncrementedMessage::class.messageDefinition.type)
            assertThat(sentEvents[2].type).isEqualTo(DecrementedMessage::class.messageDefinition.type)
        }
    }

    class TestAggregate(
        id: AggregateId,
        version: Long,
        stream: List<Facts> = emptyList(),
    ) : AbstractAggregate<TestAggregate.Facts>(id, version, stream) {

        var counter: Int = 0
        var savedVersion: Long? = null

        companion object {
            fun new() = TestAggregate(AggregateId(UUID.randomUUID().toString()), 0)
        }

        fun increment(now: Instant) {
            record(Facts.Incremented(now))
        }

        fun decrement(now: Instant) {
            record(Facts.Decremented(now))
        }

        sealed class Facts : Fact() {
            @JsonTypeName("INCREMENTED")
            data class Incremented(
                override val timestamp: Instant
            ) : Facts()

            @JsonTypeName("DECREMENTED")
            data class Decremented(
                override val timestamp: Instant
            ) : Facts()
        }

        override fun mutate(fact: Facts) {
            when (fact) {
                is Facts.Incremented -> counter++
                is Facts.Decremented -> counter--
            }
        }

        override fun mapFactToEvent(fact: Facts): List<DurableMessage> {
            val event = when (fact) {
                is Facts.Incremented -> IncrementedMessage(DurableMessageId(), fact.timestamp)
                is Facts.Decremented -> DecrementedMessage(DurableMessageId(), fact.timestamp)
            }

            return listOf(event)
        }
    }

    @MessageDefinition("IncrementedEventV1", "test-topic")
    data class IncrementedMessage(
        override val id: DurableMessageId = DurableMessageId(),
        override val timestamp: Instant
    ) : DurableMessage()

    @MessageDefinition("DecrementedEventV1", "test-topic")
    data class DecrementedMessage(
        override val id: DurableMessageId = DurableMessageId(),
        override val timestamp: Instant
    ) : DurableMessage()

    class TestAggregateRepository(
        component: AggregatesComponent
    ) : AbstractAggregateRepository<TestAggregate, TestAggregate.Facts>(component) {

        override val aggregateType: String = "TEST"

        override suspend fun doSave(aggregate: TestAggregate, version: Long, unitOfWork: UnitOfWork) {
            aggregate.savedVersion = version
        }

        override suspend fun doLoad(id: AggregateId, version: Long, facts: List<TestAggregate.Facts>): TestAggregate {
            return TestAggregate(id, version, facts)
        }
    }

    class TestOutgoingMessageTransport : OutgoingMessageTransport {
        val sentMessages = mutableListOf<OutgoingMessage>()

        override suspend fun send(messages: List<OutgoingMessage>, unitOfWork: UnitOfWork) {
            sentMessages.addAll(messages)
        }
    }
}