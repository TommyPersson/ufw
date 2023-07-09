package io.tpersson.ufw.keyvaluestore

import io.tpersson.ufw.core.UFWObjectMapper
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.keyvaluestore.storageengine.EntryDataForWrite
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Duration
import java.time.InstantSource

@Singleton
public class KeyValueStoreImpl @Inject constructor(
    private val storageEngine: StorageEngine,
    private val clock: InstantSource,
    ufwObjectMapper: UFWObjectMapper,
) : KeyValueStore {

    private val objectMapper = ufwObjectMapper.objectMapper

    override suspend fun <T> get(key: KeyValueStore.Key<T>): KeyValueStore.Entry<T>? {
        val data = storageEngine.get(key.name)
            ?: return null

        if (data.expiresAt != null && data.expiresAt < clock.instant()) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val parsedValue = objectMapper.readValue(data.json, key.type.java) as T

        return KeyValueStore.Entry(key, parsedValue, data.version, data.expiresAt)
    }

    override suspend fun <T> put(key: KeyValueStore.Key<T>, value: T, expectedVersion: Int?, ttl: Duration?, unitOfWork: UnitOfWork?) {
        val json = objectMapper.writeValueAsString(value)

        val expiresAt = ttl?.let { clock.instant() + ttl }

        val data = EntryDataForWrite(
            json = json,
            expiresAt = expiresAt,
            updatedAt = clock.instant()
        )

        storageEngine.put(key.name, data, expectedVersion, unitOfWork)
    }
}