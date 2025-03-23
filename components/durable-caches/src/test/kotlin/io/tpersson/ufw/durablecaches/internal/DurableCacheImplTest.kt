package io.tpersson.ufw.durablecaches.internal

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.KeyValueStoreImpl
import io.tpersson.ufw.keyvaluestore.storageengine.InMemoryStorageEngine
import io.tpersson.ufw.test.TestInstantSource
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import io.tpersson.ufw.test.isEqualToIgnoringNanos
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class DurableCacheImplTest {

    private lateinit var keyValueStore: KeyValueStore

    private lateinit var clock: TestInstantSource

    @BeforeEach
    fun setUp() {
        clock = TestInstantSource()
        keyValueStore = KeyValueStoreImpl(
            storageEngine = InMemoryStorageEngine(),
            clock = clock,
            objectMapper = CoreComponent.defaultObjectMapper
        )
    }

    @Test
    fun `get - Returns null for missing keys`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)

        val result = cache.get("test-1")

        assertThat(result).isNull()
    }

    @Test
    fun `get,put - Returns a wrapped value for existing keys`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)
        val key = "test-1"

        val now = clock.instant()

        cache.put(key, true)
        val result = cache.get(key)!!

        assertThat(result.key).isEqualTo(key)
        assertThat(result.value).isTrue()
        assertThat(result.cachedAt).isEqualToIgnoringNanos(now)
        assertThat(result.expiresAt).isEqualToIgnoringNanos(now.plus(Caches.PrimitiveCache.expiration!!))
    }

    @Test
    fun `getOrPut - Uses factory to put initial value`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)
        val key = "test-1"

        val now = clock.instant()

        val result = cache.getOrPut(key) { true }

        assertThat(result.key).isEqualTo(key)
        assertThat(result.value).isTrue()
        assertThat(result.cachedAt).isEqualToIgnoringNanos(now)
        assertThat(result.expiresAt).isEqualToIgnoringNanos(now.plus(Caches.PrimitiveCache.expiration!!))
    }

    @Test
    fun `put - Updates the expiration time`(): Unit = runBlocking {
        val cache = cacheOf(Caches.PrimitiveCache)
        val key = "test-1"

        clock.advance(1.minutes)
        cache.put(key, true)
        clock.advance(2.minutes)
        cache.put(key, false)
        clock.advance(3.minutes)
        cache.put(key, true)

        val now = clock.instant()

        val result = cache.get(key)!!

        assertThat(result.expiresAt).isEqualToIgnoringNanos(now.plus(Caches.PrimitiveCache.expiration!!))
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

    private fun <TValue : Any> cacheOf(definition: DurableCacheDefinition<TValue>) = DurableCacheImpl(
        definition = definition,
        keyValueStore = keyValueStore,
    )

    object Caches {
        val PrimitiveCache: DurableCacheDefinition<Boolean> = DurableCacheDefinition(
            id = "primitives",
            title = "Primitives",
            description = "Primitives",
            valueType = Boolean::class,
            expiration = Duration.ofMinutes(10),
            inMemoryExpiration = Duration.ofSeconds(30),
        )
    }
}