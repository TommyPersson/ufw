package io.tpersson.ufw.keyvaluestore.storageengine

import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProvider
import io.tpersson.ufw.db.jdbc.asMaps
import io.tpersson.ufw.db.jdbc.useInTransaction
import io.tpersson.ufw.db.unitofwork.UnitOfWork
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.postgresql.util.PGobject
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant

@Singleton
public class PostgresStorageEngine @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val connectionProvider: ConnectionProvider,
    private val config: DbModuleConfig
) : StorageEngine {

    public companion object {
        private const val TableName: String = "ufw__key_value_store"
    }

    init {
        runBlocking(config.ioContext) {
            // TODO migrations?
            connectionProvider.get().useInTransaction { conn ->
                conn.prepareStatement(
                    """
                CREATE TABLE IF NOT EXISTS $TableName
                (
                    key        TEXT  NOT NULL PRIMARY KEY,
                    value      JSONB NOT NULL,
                    expires_at TIMESTAMPTZ,
                    version    INT
                );
                """.trimIndent()
                ).executeUpdate()
            }
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

        val expiresAtTimestamp = entry.expiresAt?.let { Timestamp.from(it) }

        unitOfWork.add { conn ->
            conn.prepareStatement(
                """
                INSERT INTO $TableName AS t (
                    key,
                    value,
                    expires_at,
                    version)
                VALUES
                    (?, ?::jsonb, ?, 1)
                ON CONFLICT (key) DO UPDATE
                    SET value      = ?::jsonb,
                        expires_at = ?,
                        version    = t.version + 1
                    WHERE t.version = ? OR ? IS NULL
                """.trimIndent()
            ).also {
                it.setString(1, key)
                it.setString(2, entry.json)
                it.setTimestamp(3, expiresAtTimestamp)

                it.setString(4, entry.json)
                it.setTimestamp(5, expiresAtTimestamp)

                if (expectedVersion != null) {
                    it.setInt(6, expectedVersion)
                    it.setInt(7, expectedVersion)
                } else {
                    it.setNull(6, Types.INTEGER)
                    it.setNull(7, Types.INTEGER)
                }
            }
        }
    }

    override suspend fun deleteExpiredEntries(now: Instant, unitOfWork: UnitOfWork) {
        unitOfWork.add { conn ->
            conn.prepareStatement("DELETE FROM $TableName WHERE expires_at < ?").also {
                it.setTimestamp(1, Timestamp.from(now))
            }
        }
    }

    public suspend fun debugTruncate(): Unit = io {
        connectionProvider.get().useInTransaction {
            it.prepareStatement("DELETE FROM $TableName").executeUpdate()
        }
    }

    public suspend fun debugDumpTable(): List<Map<String, Any?>> = io {
        connectionProvider.get().prepareStatement("SELECT * FROM $TableName").executeQuery().asMaps()
    }

    private suspend fun <T> io(block: () -> T): T {
        return withContext(currentCoroutineContext() + config.ioContext) { block() }
    }
}