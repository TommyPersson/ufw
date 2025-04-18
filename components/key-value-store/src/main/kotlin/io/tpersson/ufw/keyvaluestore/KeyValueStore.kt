package io.tpersson.ufw.keyvaluestore

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
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

    public suspend fun <TValue> remove(
        key: Key<TValue>,
        unitOfWork: UnitOfWork? = null,
    )

    public suspend fun removeAll(
        keyPrefix: String,
        unitOfWork: UnitOfWork? = null,
    )

    public suspend fun list(prefix: String, paginationOptions: PaginationOptions): PaginatedList<UnparsedEntry>

    public suspend fun listMetadata(prefix: String, paginationOptions: PaginationOptions): PaginatedList<EntryMetadata>

    public suspend fun getNumEntries(keyPrefix: String): Long

    public interface UnparsedEntry {
        public val key: String
        public val value: EntryValue
        public val version: Int
        public val expiresAt: Instant?
        public val updatedAt: Instant
        public val createdAt: Instant
        public fun <T : Any?> parseAs(type: KClass<T & Any>): Entry<T>
    }

    public class Entry<T : Any?>(
        public val key: Key<T>,
        public val value: T,
        public val version: Int,
        public val expiresAt: Instant?,
        public val updatedAt: Instant,
        public val createdAt: Instant,
    )

    public class EntryMetadata(
        public val key: String,
        public val version: Int,
        public val expiresAt: Instant?,
        public val updatedAt: Instant,
        public val createdAt: Instant,
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
