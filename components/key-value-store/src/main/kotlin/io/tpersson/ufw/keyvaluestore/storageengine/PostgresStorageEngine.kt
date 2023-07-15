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
        return database.select(Queries.Selects.GetEntryByKey(key))
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

        unitOfWork.add(
            Queries.Updates.Put(
                key = key,
                value = entry.json,
                expiresAt = entry.expiresAt,
                updatedAt = entry.updatedAt,
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

    @Suppress("unused")
    private object Queries {
        object Selects {
            class GetEntryByKey(val key: String) : TypedSelect<EntryDataFromRead>(
                "SELECT *, value as json FROM $TableName WHERE key = :key"
            )

            object DebugDumpTable : TypedSelect<Map<String, Any?>>("SELECT * FROM $TableName")
        }

        object Updates {
            class Put(
                val key: String,
                val value: String,
                val expiresAt: Instant?,
                val updatedAt: Instant,
                val expectedVersion: Int?,
            ) : TypedUpdate(
                """
                INSERT INTO $TableName AS t (key, value, expires_at, updated_at, version)
                VALUES (:key, :value::jsonb, :expiresAt, :updatedAt, 1)
                ON CONFLICT (key) DO UPDATE
                    SET value      = :value::jsonb,
                        expires_at = :expiresAt,
                        updated_at = :updatedAt,
                        version    = t.version + 1
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
