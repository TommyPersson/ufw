package io.tpersson.ufw.keyvaluestore

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.managed.ManagedComponent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

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

        val dataSource = HikariDataSource(config)
        val testClock = TestInstantSource()
        val coreComponent = CoreComponent.create(testClock)
        val databaseComponent = DatabaseComponent.create(coreComponent, dataSource)
        val unitOfWorkFactory = databaseComponent.unitOfWorkFactory
        val managedComponent = ManagedComponent.create(emptySet())
        val keyValueStoreComponent = KeyValueStoreComponent.create(coreComponent, databaseComponent, managedComponent)
        val storageEngine = keyValueStoreComponent.storageEngine as PostgresStorageEngine
        val keyValueStore = keyValueStoreComponent.keyValueStore

        init {
            databaseComponent.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        storageEngine.debugTruncate()
    }

    @Test
    fun `Basic - Store and read simple value`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong!")

        val entry = keyValueStore.get(key)

        assertThat(entry).isNotNull()
        assertThat(entry!!.value).isEqualTo("Pong!")
    }

    @Test
    fun `Basic - Store and read null value`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String?>("ping")

        keyValueStore.put(key, null)

        val entry = keyValueStore.get(key)

        assertThat(entry).isNotNull()
        assertThat(entry!!.value).isNull()
    }

    @Test
    fun `Basic - Store and read complex value`(): Unit = runBlocking {
        data class MyType(val content: String)

        val key = KeyValueStore.Key.of<MyType>("my-type")

        val value = MyType("some content")

        keyValueStore.put(key, value)

        val entry = keyValueStore.get(key)

        assertThat(entry).isNotNull()
        assertThat(entry!!.value).isEqualTo(value)
    }

    @Test
    fun `Basic - Store and read ByteArray value`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<ByteArray>("my-type")

        val value = byteArrayOf(1, 2, 3, 4, 5)

        keyValueStore.put(key, value)

        val entry = keyValueStore.get(key)

        assertThat(entry).isNotNull()
        assertThat(entry!!.value).isEqualTo(value)

        // Make sure it's stored properly
        val row = storageEngine.debugDumpTable().single()
        assertThat(row["bytes"]).isEqualTo(value)
    }

    @Test
    fun `Transactions - Participates in 'UnitOfWork'`(): Unit = runBlocking {
        val uow = unitOfWorkFactory.create()

        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong!", unitOfWork = uow)

        assertThat(keyValueStore.get(key)).isNull()

        uow.commit()

        assertThat(keyValueStore.get(key)).isNotNull()
    }

    @Test
    fun `Versioning - Starts at 1 for new entries`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong!")

        assertThat(keyValueStore.get(key)!!.version).isEqualTo(1)
    }

    @Test
    fun `Versioning - Increments on each put`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong! 1")
        keyValueStore.put(key, "Pong! 2")
        keyValueStore.put(key, "Pong! 3")

        assertThat(keyValueStore.get(key)!!.version).isEqualTo(3)
    }

    @Test
    fun `Versioning - Fails if current version is not expected version`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong! 1")
        keyValueStore.put(key, "Pong! 2", expectedVersion = 1)

        assertThatThrownBy {
            runBlocking {
                keyValueStore.put(key, "Pong! 3", expectedVersion = 3)
            }
        }.isNotNull()
    }

    @Test
    fun `Versioning - Put with expected version succeeds even if no entry exists`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong! 1", expectedVersion = 1337)

        assertThat(keyValueStore.get(key)!!.version).isEqualTo(1)
    }

    @Test
    fun `Expiration - Returns null for expired entries`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong! 1", ttl = Duration.ofSeconds(5))

        assertThat(keyValueStore.get(key)).isNotNull()

        testClock.advance(Duration.ofSeconds(6))

        assertThat(keyValueStore.get(key)).isNull()
    }

    @Test
    fun `Expiration - StorageEngine needs to be told to actually delete the expired entries`(): Unit = runBlocking {
        val key = KeyValueStore.Key.of<String>("ping")

        keyValueStore.put(key, "Pong! 1", ttl = Duration.ofSeconds(5))

        testClock.advance(Duration.ofSeconds(6))

        assertThat(storageEngine.debugDumpTable()).isNotEmpty()

        storageEngine.deleteExpiredEntries(testClock.instant())

        assertThat(storageEngine.debugDumpTable()).isEmpty()
    }

    class TestInstantSource : InstantSource {
        private var now = Instant.now()

        override fun instant(): Instant {
            return now
        }

        fun advance(duration: Duration) {
            now += duration
        }
    }
}