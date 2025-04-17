package io.tpersson.ufw.keyvaluestore

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.keyvaluestore.dsl.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.test.TestClock
import io.tpersson.ufw.test.isEqualToIgnoringNanos
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration

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

        val ufw = UFW.build {
            installCore {
                clock = TestClock()
            }
            installDatabase {
                dataSource = HikariDataSource(config)
            }
            installKeyValueStore()
        }

        val testClock = ufw.core.clock as TestClock
        val unitOfWorkFactory = ufw.database.unitOfWorkFactory
        val storageEngine = ufw.keyValueStore.storageEngine as PostgresStorageEngine
        val keyValueStore = ufw.keyValueStore.keyValueStore

        init {
            ufw.database.migrator.run()
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
    fun `Remove - Deletes the key`(): Unit = runBlocking {
        val key1 = "key-1".asStringKey()
        val key2 = "key-2".asStringKey()

        keyValueStore.put(key1, "1")
        keyValueStore.put(key2, "2")

        keyValueStore.remove(key1)

        assertThat(keyValueStore.get(key1)).isNull()
        assertThat(keyValueStore.get(key2)).isNotNull()
    }

    @Test
    fun `RemoveAll - Deletes all entries matching the key prefix`(): Unit = runBlocking {
        keyValueStore.put("test-1".asStringKey(), "1")
        keyValueStore.put("test-2".asStringKey(), "1")
        keyValueStore.put("test-3".asStringKey(), "1")
        keyValueStore.put("test-4".asStringKey(), "1")
        keyValueStore.put("not-test-1".asStringKey(), "1")
        keyValueStore.put("not-test-2".asStringKey(), "1")

        keyValueStore.removeAll("test")

        val entries = keyValueStore.list("", limit = 100, offset = 0).map { it.key }.toSet()

        assertThat(entries).isEqualTo(setOf("not-test-1", "not-test-2"))
    }

    @Test
    fun `Timestamps - createdAt is only set for initial creation`(): Unit = runBlocking {
        val key = "entry".asStringKey()

        val time1 = testClock.instant()

        keyValueStore.put(key, "1")

        testClock.advance(Duration.ofMinutes(1))

        keyValueStore.put(key, "2")

        val entry = keyValueStore.get(key)!!
        assertThat(entry.createdAt).isEqualToIgnoringNanos(time1)
    }

    @Test
    fun `Timestamps - updatedAt is updated for each update`(): Unit = runBlocking {
        val key = "entry".asStringKey()

        keyValueStore.put(key, "1")

        testClock.advance(Duration.ofMinutes(1))
        val time2 = testClock.instant()

        keyValueStore.put(key, "2")

        val entry = keyValueStore.get(key)!!
        assertThat(entry.updatedAt).isEqualToIgnoringNanos(time2)
    }

    @Test
    fun `Transactions - 'put' Participates in 'UnitOfWork'`(): Unit = runBlocking {
        val uow = unitOfWorkFactory.create()

        val key = "ping".asStringKey()

        keyValueStore.put(key, "Pong!", unitOfWork = uow)

        assertThat(keyValueStore.get(key)).isNull()

        uow.commit()

        assertThat(keyValueStore.get(key)).isNotNull()
    }

    @Test
    fun `Transactions - 'remove' Participates in 'UnitOfWork'`(): Unit = runBlocking {
        val key = "ping".asStringKey()

        keyValueStore.put(key, "Pong!")

        val uow = unitOfWorkFactory.create()

        keyValueStore.remove(key, unitOfWork = uow)

        assertThat(keyValueStore.get(key)).isNotNull()

        uow.commit()

        assertThat(keyValueStore.get(key)).isNull()
    }

    @Test
    fun `Versioning - Starts at 1 for new entries`(): Unit = runBlocking {
        val key = "ping".asStringKey()

        keyValueStore.put(key, "Pong!")

        assertThat(keyValueStore.get(key)!!.version).isEqualTo(1)
    }

    @Test
    fun `Versioning - Increments on each put`(): Unit = runBlocking {
        val key = "ping".asStringKey()

        keyValueStore.put(key, "Pong! 1")
        keyValueStore.put(key, "Pong! 2")
        keyValueStore.put(key, "Pong! 3")

        assertThat(keyValueStore.get(key)!!.version).isEqualTo(3)
    }

    @Test
    fun `Versioning - Fails if current version is not expected version`(): Unit = runBlocking {
        val key = "ping".asStringKey()

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
        val key = "ping".asStringKey()

        keyValueStore.put(key, "Pong! 1", expectedVersion = 1337)

        assertThat(keyValueStore.get(key)!!.version).isEqualTo(1)
    }

    @Test
    fun `Expiration - Returns null for expired entries`(): Unit = runBlocking {
        val key = "ping".asStringKey()

        keyValueStore.put(key, "Pong! 1", ttl = Duration.ofSeconds(5))

        assertThat(keyValueStore.get(key)).isNotNull()

        testClock.advance(Duration.ofSeconds(6))

        assertThat(keyValueStore.get(key)).isNull()
    }

    @Test
    fun `Expiration - StorageEngine needs to be told to actually delete the expired entries`(): Unit = runBlocking {
        val key = "ping".asStringKey()

        keyValueStore.put(key, "Pong! 1", ttl = Duration.ofSeconds(5))

        testClock.advance(Duration.ofSeconds(6))

        assertThat(storageEngine.debugDumpTable()).isNotEmpty()

        storageEngine.deleteExpiredEntries(testClock.instant())

        assertThat(storageEngine.debugDumpTable()).isEmpty()
    }

    @Test
    fun `Search - Can list entries by prefix`(): Unit = runBlocking {
        keyValueStore.put("counter:1".asIntKey(), 1)
        keyValueStore.put("counter:2".asIntKey(), 2)
        keyValueStore.put("counter:3".asIntKey(), 3)
        keyValueStore.put("counter:4".asIntKey(), 4)
        keyValueStore.put("counter:5".asIntKey(), 5)
        keyValueStore.put("counter:6".asIntKey(), 6)
        keyValueStore.put("not-counter:7".asIntKey(), 7)

        val entries1 = keyValueStore.list("counter:", limit = 3, offset = 0)
        assertThat(entries1).hasSize(3)
        assertThat(entries1[0].parseAs(Int::class).value).isEqualTo(1)
        assertThat(entries1[1].parseAs(Int::class).value).isEqualTo(2)
        assertThat(entries1[2].parseAs(Int::class).value).isEqualTo(3)

        val entries2 = keyValueStore.list("counter:", limit = 100, offset = 4)
        assertThat(entries2).hasSize(2)
        assertThat(entries2[0].parseAs(Int::class).value).isEqualTo(5)
        assertThat(entries2[1].parseAs(Int::class).value).isEqualTo(6)
    }

    @Test
    fun `getNumEntries - Count entries by prefix`(): Unit = runBlocking {
        keyValueStore.put("counter:1".asIntKey(), 1)
        keyValueStore.put("counter:2".asIntKey(), 2)
        keyValueStore.put("counter:3".asIntKey(), 3)
        keyValueStore.put("counter:4".asIntKey(), 4)
        keyValueStore.put("counter:5".asIntKey(), 5)
        keyValueStore.put("counter:6".asIntKey(), 6)
        keyValueStore.put("not-counter:7".asIntKey(), 7)

        assertThat(keyValueStore.getNumEntries("counter:")).isEqualTo(6)
        assertThat(keyValueStore.getNumEntries("not-counter:")).isEqualTo(1)
    }

    fun String.asStringKey() = KeyValueStore.Key.of<String>(this)
    fun String.asIntKey() = KeyValueStore.Key.of<Int>(this)
}