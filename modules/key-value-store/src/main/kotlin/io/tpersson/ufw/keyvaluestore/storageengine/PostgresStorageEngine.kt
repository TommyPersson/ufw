package io.tpersson.ufw.keyvaluestore.storageengine

import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProvider
import io.tpersson.ufw.db.jdbc.asMaps
import io.tpersson.ufw.db.jdbc.useInTransaction
import io.tpersson.ufw.db.typedqueries.TypedUpdate
import io.tpersson.ufw.db.unitofwork.UnitOfWork
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.postgresql.util.PGobject
import java.sql.Timestamp
import java.time.Instant

@Singleton
public class PostgresStorageEngine @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val connectionProvider: ConnectionProvider,
    private val config: DbModuleConfig
) : StorageEngine {

    // TODO custom exception for expectedVersion mismatch?

    public companion object {
        private const val TableName: String = "ufw__key_value_store"
    }

    init {
        Flyway.configure()
            .dataSource(connectionProvider.dataSource)
            .loggers("slf4j")
            .baselineOnMigrate(true)
            .locations("classpath:io/tpersson/ufw/keyvaluestore/migrations/postgres")
            .table("ufw__key_value_store__flyway")
            .load().also {
                it.migrate()
            }
    }

    override suspend fun get(
        key: String
    ): EntryDataFromRead? = io {
        val data = connectionProvider.get().useInTransaction { conn ->
            conn.prepareStatement("SELECT * FROM $TableName WHERE key = ?").also {
                it.setString(1, key)
            }.executeQuery().asMaps().singleOrNull()
        } ?: return@io null

        EntryDataFromRead(
            json = (data["value"] as PGobject).value!!,
            expiresAt = data["expires_at"] as Instant?,
            updatedAt = data["updated_at"] as Instant,
            version = data["version"] as Int
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

        unitOfWork.add(
            Queries.Put(
                key = key,
                value = entry.json,
                expiresAt = entry.expiresAt,
                updatedAt = entry.updatedAt,
                expectedVersion = expectedVersion
            )
        )
    }

    override suspend fun deleteExpiredEntries(now: Instant, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.DeleteAllExpired(now))
    }

    public suspend fun debugTruncate(): Unit = io {
        connectionProvider.get().useInTransaction {
            Queries.TruncateTable().asPreparedStatement(it).executeUpdate()
        }
    }

    public suspend fun debugDumpTable(): List<Map<String, Any?>> = io {
        connectionProvider.get().prepareStatement("SELECT * FROM $TableName").executeQuery().asMaps()
    }

    private suspend fun <T> io(block: () -> T): T {
        return withContext(currentCoroutineContext() + config.ioContext) { block() }
    }

    private object Queries {
        class Put(
            val key: String,
            val value: String,
            val expiresAt: Instant?,
            val updatedAt: Instant,
            val expectedVersion: Int?,
        ) : TypedUpdate(
            """
            INSERT INTO $TableName AS t (
                    key,
                    value,
                    expires_at,
                    updated_at,
                    version)
                VALUES
                    (:key, :value::jsonb, :expiresAt, :updatedAt, 1)
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

        class TruncateTable() : TypedUpdate("DELETE FROM $TableName", minimumAffectedRows = 0)
    }
}
