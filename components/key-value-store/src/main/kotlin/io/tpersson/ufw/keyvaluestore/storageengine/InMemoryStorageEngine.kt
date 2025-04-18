package io.tpersson.ufw.keyvaluestore.storageengine

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
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

    override suspend fun remove(key: String, unitOfWork: UnitOfWork?) {
        if (unitOfWork != null) {
            unitOfWork.addPostCommitHook {
                storage.remove(key)
            }
        } else {
            storage.remove(key)
        }
    }

    override suspend fun removeAll(keyPrefix: String, unitOfWork: UnitOfWork?) {
        val keysToRemove = storage.values
            .filter { entry -> entry.key.startsWith(keyPrefix) }
            .map { it.key }

        if (unitOfWork != null) {
            unitOfWork.addPostCommitHook {
                doRemoveAll(keysToRemove)
            }
        } else {
            doRemoveAll(keysToRemove)
        }
    }

    override suspend fun deleteExpiredEntries(now: Instant): Int {
        val keysToRemove = storage.values
            .filter { entry -> entry.expiresAt != null && entry.expiresAt > now }
            .map { it.key }

        doRemoveAll(keysToRemove)

        return keysToRemove.size
    }

    override suspend fun list(prefix: String, paginationOptions: PaginationOptions): PaginatedList<EntryDataFromRead> {
        val items = storage.values
            .sortedBy { it.key }
            .filter { it.key.startsWith(prefix) }
            .drop(paginationOptions.offset)
            .take(paginationOptions.limit + 1)

        return PaginatedList(
            items = items.take(paginationOptions.limit),
            options = paginationOptions,
            hasMoreItems = items.size > paginationOptions.limit
        )
    }

    override suspend fun listMetadata(prefix: String, paginationOptions: PaginationOptions): PaginatedList<EntryMetadata> {
        return list(prefix, paginationOptions).map {
            EntryMetadata(
                key = it.key,
                expiresAt = it.expiresAt,
                updatedAt = it.updatedAt,
                createdAt = it.createdAt,
                version = it.version,
            )
        }
    }

    override suspend fun getNumEntries(keyPrefix: String): Long {
        return storage.values.filter { it.key.startsWith(keyPrefix) }.size.toLong()
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

    private fun doRemoveAll(keysToRemove: List<String>) {
        for (key in keysToRemove) {
            storage.remove(key)
        }
    }
}