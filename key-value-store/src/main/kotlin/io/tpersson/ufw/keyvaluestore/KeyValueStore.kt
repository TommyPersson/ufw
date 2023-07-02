package io.tpersson.ufw.keyvaluestore

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tpersson.ufw.db.unitofwork.UnitOfWork
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

public fun interface ClockProvider {
    public fun provide(): Clock
}

public interface KeyValueStore {

    public suspend fun <T : Any?> get(key: Key<T>): Entry<T>?

    public suspend fun <T : Any?> put(
        key: Key<T>,
        value: T,
        expectedVersion: Int? = null,
        ttl: Duration? = null,
        unitOfWork: UnitOfWork? = null
    )

    public class Entry<T : Any?>(
        public val key: Key<T>,
        public val value: T,
        public val version: Int,
        public val expiresAt: Instant?,
    )

    public class Key<T : Any?>(
        public val name: String,
        public val type: KClass<out Any>,
    ) {
        public companion object {
            public inline fun <reified T : Any?> of(name: String): Key<T> {
                @Suppress("UNCHECKED_CAST")
                return Key(name, T::class as KClass<out Any>)
            }
        }
    }

    public companion object {
        public fun create(
            storage: StorageEngine,
            clockProvider: ClockProvider = ClockProvider { Clock.systemUTC() },
            objectMapper: ObjectMapper = defaultObjectMapper
        ): KeyValueStore {
            return KeyValueStoreImpl(storage, clockProvider, objectMapper)
        }

        public val defaultObjectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
    }
}
