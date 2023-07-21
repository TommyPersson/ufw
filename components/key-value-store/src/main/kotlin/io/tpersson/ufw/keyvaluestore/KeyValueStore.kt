package io.tpersson.ufw.keyvaluestore

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.keyvaluestore.storageengine.EntryValue
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

public interface KeyValueStore {

    public suspend fun <T : Any?> get(key: Key<T>): Entry<T>?

    public suspend fun <T : Any?> put(
        key: Key<T>,
        value: T,
        expectedVersion: Int? = null,
        ttl: Duration? = null,
        unitOfWork: UnitOfWork? = null
    )

    public suspend fun list(prefix: String, limit: Int, offset: Int = 0): List<UnparsedEntry>

    public interface UnparsedEntry {
        public val key: String
        public val value: EntryValue
        public val version: Int
        public val expiresAt: Instant?
        public fun <T : Any?> parseAs(type: KClass<T & Any>): Entry<T>
    }

    public class Entry<T : Any?>(
        public val key: Key<T>,
        public val value: T,
        public val version: Int,
        public val expiresAt: Instant?,
    )

    public class Key<T : Any?>(
        public val name: String,
        public val type: KClass<out T & Any>,
    ) {
        public companion object {
            public inline fun <reified T : Any?> of(name: String): Key<T> {
                @Suppress("UNCHECKED_CAST")
                return Key(name, T::class as KClass<out T & Any>)
            }
        }
    }
}
