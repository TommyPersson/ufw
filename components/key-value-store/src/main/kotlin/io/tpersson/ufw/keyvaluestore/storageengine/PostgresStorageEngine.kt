package io.tpersson.ufw.keyvaluestore.storageengine

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Instant

@Singleton
public class PostgresStorageEngine @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val database: Database
) : StorageEngine {

    // TODO custom exception for expectedVersion mismatch?

    public companion object {
        private const val TableName: String = "ufw__key_value_store"
    }

    override suspend fun get(key: String): EntryDataFromRead? {
        val data = database.select(Queries.Selects.GetEntryByKey(key))
            ?: return null

        val value = when (data.type) {
            EntryType.Json.int -> EntryValue.Json(data.json!!)
            EntryType.Bytes.int -> EntryValue.Bytes(data.bytes!!)
            else -> TODO()
        }

        return EntryDataFromRead(
            value = value,
            expiresAt = data.expiresAt,
            updatedAt = data.updatedAt,
            version = data.version
        )
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
            updatedAt = entry.updatedAt,
            version = 0 // Doesn't matter
        )

        unitOfWork.add(
            Queries.Updates.Put(
                data = data,
                expectedVersion = expectedVersion
            )
        )
    }

    override suspend fun deleteExpiredEntries(now: Instant): Int {
        return database.update(Queries.Updates.DeleteAllExpired(now))
    }

    public suspend fun debugTruncate(): Unit {
        database.update(Queries.Updates.TruncateTable)
    }

    public suspend fun debugDumpTable(): List<Map<String, Any?>> {
        return database.selectList(Queries.Selects.DebugDumpTable)
    }

    internal data class EntryData(
        val key: String,
        val type: Int,
        val json: String?,
        val bytes: ByteArray?,
        val expiresAt: Instant?,
        val updatedAt: Instant,
        val version: Int
    )

    @Suppress("unused")
    private object Queries {
        object Selects {
            class GetEntryByKey(val key: String) : TypedSelect<EntryData>(
                "SELECT * FROM $TableName WHERE key = :key"
            )

            object DebugDumpTable : TypedSelect<Map<String, Any?>>("SELECT * FROM $TableName")
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
                    version
                ) VALUES (
                    :data.key, 
                    :data.type, 
                    :data.json::jsonb, 
                    :data.bytes::bytea, 
                    :data.expiresAt, 
                    :data.updatedAt, 
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

            class DeleteAllExpired(
                val now: Instant
            ) : TypedUpdate("DELETE FROM $TableName WHERE expires_at < :now", minimumAffectedRows = 0)

            object TruncateTable : TypedUpdate("DELETE FROM $TableName", minimumAffectedRows = 0)
        }
    }
}
