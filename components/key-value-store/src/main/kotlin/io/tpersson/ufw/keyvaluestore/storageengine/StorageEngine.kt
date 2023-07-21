package io.tpersson.ufw.keyvaluestore.storageengine

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

    public suspend fun deleteExpiredEntries(
        now: Instant
    ): Int

    public suspend fun list(prefix: String, limit: Int, offset: Int): List<EntryDataFromRead>
}

