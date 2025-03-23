package io.tpersson.ufw.keyvaluestore.storageengine

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

// TODO tests

public class InMemoryStorageEngine : StorageEngine {

    private val storage = ConcurrentHashMap<String, EntryDataFromRead>()

    override suspend fun get(key: String): EntryDataFromRead? {
        return storage[key]
    }

    override suspend fun put(key: String, entry: EntryDataForWrite, expectedVersion: Int?, unitOfWork: UnitOfWork?) {
        if (unitOfWork != null) {
            unitOfWork.addPostCommitHook {
                doPut(key, entry)
            }
        } else {
            doPut(key, entry)
        }
    }

    override suspend fun deleteExpiredEntries(now: Instant): Int {
        val keysToRemove = storage.values
            .filter { entry -> entry.expiresAt != null && entry.expiresAt > now }
            .map { it.key }

        for (key in keysToRemove) {
            storage.remove(key)
        }

        return keysToRemove.size
    }

    override suspend fun list(prefix: String, limit: Int, offset: Int): List<EntryDataFromRead> {
        return storage.values
            .filter { it.key.startsWith(prefix) }
            .drop(offset)
            .take(limit)
    }

    private fun doPut(key: String, entry: EntryDataForWrite) {
        val existing = storage[key]
        storage[key] = EntryDataFromRead(
            key = key,
            createdAt = existing?.createdAt ?: entry.now,
            updatedAt = entry.now,
            value = entry.value,
            expiresAt = entry.expiresAt,
            version = existing?.version ?: 1,
        )
    }
}