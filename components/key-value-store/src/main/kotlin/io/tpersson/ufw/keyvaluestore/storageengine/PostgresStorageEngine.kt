package io.tpersson.ufw.keyvaluestore.storageengine

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.core.utils.paginate
import io.tpersson.ufw.database.exceptions.MinimumAffectedRowsException
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.keyvaluestore.exceptions.VersionMismatchException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import java.time.Instant

@Singleton
public class PostgresStorageEngine @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val database: Database
) : StorageEngine {

    override suspend fun get(key: String): EntryDataFromRead? {
        val data = database.select(Queries.Selects.GetEntryByKey(key))
            ?: return null

        return data.asEntryDataFromRead()
    }

    override suspend fun put(
        key: String,
        entry: EntryDataForWrite,
        expectedVersion: Int?,
        unitOfWork: UnitOfWork?
    ): Unit {
        if (unitOfWork == null) {
            val uow = unitOfWorkFactory.create()
            put(key, entry, expectedVersion, uow)
            uow.commit()
            return
        }

        val data = EntryData(
            key = key,
            type = entry.value.type.int,
            json = if (entry.value is EntryValue.Json) entry.value.json else null,
            bytes = if (entry.value is EntryValue.Bytes) entry.value.bytes else null,
            expiresAt = entry.expiresAt,
            updatedAt = entry.now,
            createdAt = entry.now, // Will be ignored by update-statement
            version = 0 // Doesn't matter
        )

        unitOfWork.add(
            Queries.Updates.Put(
                data = data,
                expectedVersion = expectedVersion
            ),
            exceptionMapper(key, expectedVersion)
        )
    }

    override suspend fun remove(key: String, unitOfWork: UnitOfWork?) {
        if (unitOfWork == null) {
            val uow = unitOfWorkFactory.create()
            remove(key, uow)
            uow.commit()
            return
        }

        unitOfWork.add(Queries.Updates.Delete(key))
    }

    override suspend fun removeAll(keyPrefix: String, unitOfWork: UnitOfWork?) {
        if (unitOfWork == null) {
            val uow = unitOfWorkFactory.create()
            removeAll(keyPrefix, uow)
            uow.commit()
            return
        }

        unitOfWork.add(Queries.Updates.DeleteAllWithPrefix(keyPrefix))
    }

    override suspend fun deleteExpiredEntries(now: Instant): Int {
        return database.update(Queries.Updates.DeleteAllExpired(now))
    }

    override suspend fun list(
        prefix: String,
        paginationOptions: PaginationOptions
    ): PaginatedList<EntryDataFromRead> {
        return database.select(Queries.Selects.ListByPrefix(prefix, paginationOptions))
            .map { it.asEntryDataFromRead() }
    }

    override suspend fun listMetadata(
        prefix: String,
        paginationOptions: PaginationOptions
    ): PaginatedList<EntryMetadata> {
        return database.select(Queries.Selects.ListMetadataByPrefix(prefix, paginationOptions))
            .map { it.asEntryMetadata() }
    }

    override suspend fun getNumEntries(keyPrefix: String): Long {
        return database.select(Queries.Selects.GetNumberOfEntriesWithPrefix(keyPrefix))?.count ?: 0
    }

    public suspend fun debugTruncate(): Unit {
        database.update(Queries.Updates.TruncateTable)
    }

    public suspend fun debugDumpTable(): List<Map<String, Any?>> {
        return paginate {
            database.select(Queries.Selects.DebugDumpTable(it))
        }.toList().flatMap { it.items }
    }

    private fun exceptionMapper(key: String, expectedVersion: Int?): (Exception) -> Exception = {
        if (it is MinimumAffectedRowsException) {
            VersionMismatchException(key, expectedVersion, it)
        } else it
    }

    private fun EntryData.asEntryDataFromRead(): EntryDataFromRead {
        val value = when (type) {
            EntryType.JSON.int -> EntryValue.Json(json!!)
            EntryType.Bytes.int -> EntryValue.Bytes(bytes!!)
            else -> error("Unknown EntryValue type: $type")
        }

        return EntryDataFromRead(
            key = key,
            value = value,
            expiresAt = expiresAt,
            updatedAt = updatedAt,
            createdAt = createdAt,
            version = version
        )
    }

    private fun EntryData.asEntryMetadata(): EntryMetadata {
        return EntryMetadata(
            key = key,
            expiresAt = expiresAt,
            updatedAt = updatedAt,
            createdAt = createdAt,
            version = version
        )
    }

    internal data class EntryData(
        val key: String,
        val type: Int,
        val json: String?,
        val bytes: ByteArray?,
        val expiresAt: Instant?,
        val updatedAt: Instant,
        val createdAt: Instant,
        val version: Int
    )

    @Suppress("unused")
    private object Queries {

        private const val TableName: String = "ufw__key_value_store"

        object Selects {
            class GetEntryByKey(val key: String) : TypedSelectSingle<EntryData>(
                "SELECT * FROM $TableName WHERE key = :key"
            )

            class ListByPrefix(
                val prefix: String,
                override val paginationOptions: PaginationOptions
            ) : TypedSelectList<EntryData>(
                """
                SELECT * 
                FROM $TableName 
                WHERE key LIKE (:prefix || '%')
                ORDER BY key
                """.trimIndent()
            )

            class ListMetadataByPrefix(
                val prefix: String,
                override val paginationOptions: PaginationOptions
            ) : TypedSelectList<EntryData>(
                """
                SELECT key, type, null as json, null as bytes, expires_at, updated_at, created_at, version
                FROM $TableName 
                WHERE key LIKE (:prefix || '%')
                ORDER BY key
                """.trimIndent()
            )

            class GetNumberOfEntriesWithPrefix(
                val prefix: String,
            ) : TypedSelectSingle<Count>(
                """
                SELECT count(*) as count
                FROM $TableName 
                WHERE key LIKE (:prefix || '%')
                """.trimIndent()
            )

            class DebugDumpTable(
                override val paginationOptions: PaginationOptions
            ) : TypedSelectList<Map<String, Any?>>("SELECT * FROM $TableName")
        }

        object Updates {
            class Put(
                val data: EntryData,
                val expectedVersion: Int?,
            ) : TypedUpdate(
                """
                INSERT INTO $TableName AS t (
                    key, 
                    type, 
                    json, 
                    bytes, 
                    expires_at, 
                    updated_at,
                    created_at,
                    version
                ) VALUES (
                    :data.key, 
                    :data.type, 
                    :data.json::jsonb, 
                    :data.bytes::bytea, 
                    :data.expiresAt, 
                    :data.updatedAt,
                    :data.createdAt, 
                    1
                ) ON CONFLICT (key) DO UPDATE
                    SET json       = :data.json::jsonb,
                        bytes      = :data.bytes::bytea,
                        expires_at = :data.expiresAt,
                        updated_at = :data.updatedAt,
                        VERSION    = t.version + 1
                    WHERE t.version = :expectedVersion OR :expectedVersion IS NULL
                """.trimIndent(),
            )

            class Delete(
                val key: String
            ) : TypedUpdate(
                """
                DELETE 
                FROM $TableName 
                WHERE key = :key
                """.trimIndent(),
                minimumAffectedRows = 0
            )


            class DeleteAllWithPrefix(
                val keyPrefix: String
            ) : TypedUpdate(
                """
                DELETE 
                FROM $TableName
                WHERE key LIKE (:keyPrefix || '%')
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            class DeleteAllExpired(
                val now: Instant
            ) : TypedUpdate(
                """
                DELETE 
                FROM $TableName
                WHERE expires_at < :now
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            object TruncateTable : TypedUpdate("DELETE FROM $TableName", minimumAffectedRows = 0)
        }
    }

    internal data class Count(val count: Long)
}
