package io.tpersson.ufw.keyvaluestore.storageengine

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import java.time.Instant

public interface StorageEngine {

    public suspend fun get(
        key: String
    ): EntryDataFromRead?

    public suspend fun put(
        key: String,
        entry: EntryDataForWrite,
        expectedVersion: Int? = null,
        unitOfWork: UnitOfWork? = null
    )

    public suspend fun remove(
        key: String,
        unitOfWork: UnitOfWork? = null
    )

    public suspend fun removeAll(
        keyPrefix: String,
        unitOfWork: UnitOfWork? = null
    )

    public suspend fun deleteExpiredEntries(
        now: Instant
    ): Int

    public suspend fun list(
        prefix: String,
        paginationOptions: PaginationOptions
    ): PaginatedList<EntryDataFromRead>

    public suspend fun listMetadata(
        prefix: String,
        paginationOptions: PaginationOptions
    ): PaginatedList<EntryMetadata>

    public suspend fun getNumEntries(keyPrefix: String): Long
}

