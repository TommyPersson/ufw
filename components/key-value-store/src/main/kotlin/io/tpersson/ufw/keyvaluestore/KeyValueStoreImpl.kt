package io.tpersson.ufw.keyvaluestore

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.keyvaluestore.storageengine.EntryDataForWrite
import io.tpersson.ufw.keyvaluestore.storageengine.EntryValue
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Duration
import java.time.Instant
import java.time.Clock
import kotlin.reflect.KClass

@Singleton
public class KeyValueStoreImpl @Inject constructor(
    private val storageEngine: StorageEngine,
    private val clock: Clock,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : KeyValueStore {

    override suspend fun <T> get(key: KeyValueStore.Key<T>): KeyValueStore.Entry<T>? {
        val data = storageEngine.get(key.name)
            ?: return null

        if (data.expiresAt != null && data.expiresAt < clock.instant()) {
            return null
        }

        val parsedValue = data.value.parse(objectMapper, key.type)

        return KeyValueStore.Entry(
            key = key,
            value = parsedValue,
            version = data.version,
            expiresAt = data.expiresAt,
            updatedAt = data.updatedAt,
            createdAt = data.createdAt,
        )
    }

    override suspend fun <T> put(key: KeyValueStore.Key<T>, value: T, expectedVersion: Int?, ttl: Duration?, unitOfWork: UnitOfWork?) {
        val expiresAt = ttl?.let { clock.instant() + ttl }

        val entryValue = when (key.type) {
            ByteArray::class -> EntryValue.Bytes(value as ByteArray)
            else -> EntryValue.Json(objectMapper.writeValueAsString(value))
        }

        val data = EntryDataForWrite(
            value = entryValue,
            expiresAt = expiresAt,
            now = clock.instant(),
        )

        storageEngine.put(key.name, data, expectedVersion, unitOfWork)
    }

    override suspend fun <TValue> remove(key: KeyValueStore.Key<TValue>, unitOfWork: UnitOfWork?) {
        storageEngine.remove(key.name, unitOfWork)
    }

    override suspend fun removeAll(keyPrefix: String, unitOfWork: UnitOfWork?) {
        storageEngine.removeAll(keyPrefix, unitOfWork)
    }

    override suspend fun list(prefix: String, limit: Int, offset: Int): List<KeyValueStore.UnparsedEntry> {
        val data = storageEngine.list(prefix, limit, offset)
        return data.map {
            UnparsedEntryImpl(
                key = it.key,
                value = it.value,
                version = it.version,
                expiresAt = it.expiresAt,
                updatedAt = it.updatedAt,
                createdAt = it.createdAt,
                objectMapper = objectMapper
            )
        }
    }

    override suspend fun getNumEntries(keyPrefix: String): Long {
        return storageEngine.getNumEntries(keyPrefix)
    }

    private class UnparsedEntryImpl(
        override val key: String,
        override val value: EntryValue,
        override val version: Int,
        override val expiresAt: Instant?,
        override val updatedAt: Instant,
        override val createdAt: Instant,
        private val objectMapper: ObjectMapper,
    ) : KeyValueStore.UnparsedEntry {
        override fun <T> parseAs(type: KClass<T & Any>): KeyValueStore.Entry<T> {
            return KeyValueStore.Entry(
                key = KeyValueStore.Key(key, type),
                value = value.parse(objectMapper, type),
                version = version,
                expiresAt = expiresAt,
                updatedAt = updatedAt,
                createdAt = createdAt,
            )
        }
    }
}

private fun <T : Any?> EntryValue.parse(
    objectMapper: ObjectMapper,
    clazz: KClass<out T & Any>
): T {
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    return when (this) {
        is EntryValue.Bytes -> bytes
        is EntryValue.Json -> objectMapper.readValue(json, clazz.java)
    } as T
}