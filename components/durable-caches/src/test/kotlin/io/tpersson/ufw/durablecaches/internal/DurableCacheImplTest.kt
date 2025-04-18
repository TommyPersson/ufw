package io.tpersson.ufw.durablecaches.internal

import io.tpersson.ufw.core.component.CoreComponent
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablecaches.CacheEntry
import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.KeyValueStoreImpl
import io.tpersson.ufw.keyvaluestore.storageengine.InMemoryStorageEngine
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import io.tpersson.ufw.test.isEqualToIgnoringNanos
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes

internal class DurableCacheImplTest {

    private lateinit var keyValueStore: ObservableKeyValueStore

    private lateinit var clock: TestClock

    @BeforeEach
    fun setUp() {
        clock = TestClock(Instant.parse("2021-01-01T00:00:00Z"))
        keyValueStore = ObservableKeyValueStore(
            KeyValueStoreImpl(
                storageEngine = InMemoryStorageEngine(),
                clock = clock,
                objectMapper = CoreComponent.defaultObjectMapper
            )
        )
    }

    @Test
    fun `get - Returns null for missing keys`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)

        val result = cache.get("test-1")

        assertThat(result).isNull()
    }

    @Test
    fun `getOrPut - Uses factory to put initial value`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)
        val key = "test-1"

        val result = cache.getOrPut(key) { true }

        assertThat(result).isTrue()
    }

    @Test
    fun `put - Updates the expiresAt and cachedAt timestamps`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)
        val key = "test-1"

        clock.advance(1.minutes)
        cache.put(key, true)
        clock.advance(2.minutes)
        cache.put(key, false)
        clock.advance(3.minutes)
        cache.put(key, true)

        val now = clock.instant()

        val result = getCacheEntry(cache, key)

        assertThat(result.expiresAt).isEqualToIgnoringNanos(now.plus(Caches.PrimitiveCache.expiration!!))
        assertThat(result.cachedAt).isEqualToIgnoringNanos(now)
    }

    @Test
    fun `list - Lists all entries in the cache matching the key prefix`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)

        (1..100).forEach { i ->
            cache.put("test-${i.toString().padStart(3, '0')}", i % 2 == 0)
        }

        val result = cache.list("test-", PaginationOptions(limit = 5, offset = 5))
            .items
            .map { it.key to it.value }
            .toSet()

        assertThat(result).isEqualTo(
            setOf(
                "test-006" to true,
                "test-007" to false,
                "test-008" to true,
                "test-009" to false,
                "test-010" to true,
            )
        )
    }

    @Test
    fun `remove - Deletes a cache entry`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)
        val key = "test-1"

        cache.put(key, true)
        cache.remove(key)

        val result = cache.get(key)

        assertThat(result).isNull()
    }

    @Test
    fun `removeAll - Deletes all entries`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)

        cache.put("test-1", true)
        cache.put("test-2", true)
        cache.put("test-3", true)
        cache.put("test-4", true)

        cache.removeAll()

        val result = cache.list("", PaginationOptions.DEFAULT).items

        assertThat(result).isEmpty()
    }

    @Test
    fun `WithInMemoryCache - Stores values in an in-memory cache`(): Unit = runBlocking {
        val cache = cacheOf(Caches.WithInMemoryCache)
        val key = "test-1"

        cache.put(key, true)

        assertThat(cache.get(key)).isTrue() // + 0

        assertThat(keyValueStore.numGets.get()).isEqualTo(0)

        clock.advance(Caches.WithInMemoryCache.inMemoryExpiration!!.plusSeconds(1))

        assertThat(cache.get(key)).isTrue() // + 1

        assertThat(keyValueStore.numGets.get()).isEqualTo(1)
    }

    @Test
    fun `WithInMemoryCache - Removals affect in-memory cache`(): Unit = runBlocking {
        val cache = cacheOf(Caches.WithInMemoryCache)
        val key = "test-1"

        cache.put(key, true)

        assertThat(cache.get(key)).isTrue() // + 0

        assertThat(keyValueStore.numGets.get()).isEqualTo(0)

        cache.removeAll()

        assertThat(cache.get(key)).isNull() // + 1

        assertThat(keyValueStore.numGets.get()).isEqualTo(1)

        cache.put(key, true)

        assertThat(cache.get(key)).isTrue() // + 0

        assertThat(keyValueStore.numGets.get()).isEqualTo(1)

        cache.remove(key)

        assertThat(cache.get(key)).isNull() // + 1

        assertThat(keyValueStore.numGets.get()).isEqualTo(2)
    }

    private fun <TValue : Any> cacheOf(definition: DurableCacheDefinition<TValue>): DurableCacheImpl<TValue> {
        return DurableCacheImpl(
            definition = definition,
            keyValueStore = keyValueStore,
            clock = clock,
        )
    }

    private suspend fun getCacheEntry(
        cache: DurableCacheImpl<Boolean>,
        key: String
    ): CacheEntry<Boolean> {
        return cache.list("", PaginationOptions.DEFAULT).items.first { it.key == key }
    }

    object Caches {
        object PrimitiveCache : DurableCacheDefinition<Boolean> {
            override val id = "primitives"
            override val title = "Primitives"
            override val description = "Primitives"
            override val valueType = Boolean::class
            override val expiration = Duration.ofMinutes(10)
            override val inMemoryExpiration = null
        }

        object WithInMemoryCache : DurableCacheDefinition<Boolean> {
            override val id = "with-in-memory-cache"
            override val title = "With in-memory cache"
            override val description = "It has an in-memory cache as well"
            override val valueType = Boolean::class
            override val expiration = Duration.ofMinutes(10)
            override val inMemoryExpiration = Duration.ofSeconds(30)
        }
    }

    class ObservableKeyValueStore(
        private val inner: KeyValueStore,
    ) : KeyValueStore {

        val numGets = AtomicInteger(0)
        val numPuts = AtomicInteger(0)
        val numRemoves = AtomicInteger(0)
        val numRemoveAlls = AtomicInteger(0)
        val numLists = AtomicInteger(0)
        val numGetNumEntries = AtomicInteger(0)

        override suspend fun <T> get(key: KeyValueStore.Key<T>): KeyValueStore.Entry<T>? {
            numGets.incrementAndGet()
            return inner.get(key)
        }

        override suspend fun <T> put(
            key: KeyValueStore.Key<T>,
            value: T,
            expectedVersion: Int?,
            ttl: Duration?,
            unitOfWork: UnitOfWork?
        ) {
            numPuts.incrementAndGet()
            return inner.put(key, value, expectedVersion, ttl, unitOfWork)
        }

        override suspend fun <TValue> remove(key: KeyValueStore.Key<TValue>, unitOfWork: UnitOfWork?) {
            numRemoves.incrementAndGet()
            return inner.remove(key, unitOfWork)
        }

        override suspend fun removeAll(keyPrefix: String, unitOfWork: UnitOfWork?) {
            numRemoveAlls.incrementAndGet()
            return inner.removeAll(keyPrefix, unitOfWork)
        }

        override suspend fun list(prefix: String, limit: Int, offset: Int): List<KeyValueStore.UnparsedEntry> {
            numLists.incrementAndGet()
            return inner.list(prefix, limit, offset)
        }

        override suspend fun getNumEntries(keyPrefix: String): Long {
            numGetNumEntries.incrementAndGet()
            return inner.getNumEntries(keyPrefix)
        }
    }
}