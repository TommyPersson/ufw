package io.tpersson.ufw.database.locks.internal

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import jakarta.inject.Inject
import java.time.Duration
import java.time.Instant

public class DatabaseLocksDAO @Inject constructor(
    private val database: Database
) {
    public suspend fun acquireOrRefresh(
        lockId: String,
        ownerId: String,
        now: Instant,
        stealIfOlderThan: Duration?
    ): Boolean {
        val affectedRows = database.update(
            Queries.Updates.AcquireOrRefresh(
                id = lockId,
                owner = ownerId,
                now = now,
                stealIfOlderThan = stealIfOlderThan?.let { now - it }
            )
        )

        return affectedRows > 0
    }

    public suspend fun release(lockId: String, instanceId: String) {
        database.update(Queries.Updates.Release(lockId, instanceId))
    }

    public suspend fun truncate() {
        database.update(Queries.Updates.Truncate)
    }

    internal object Queries {
        object Updates {
            class AcquireOrRefresh(
                val id: String,
                val owner: String,
                val now: Instant,
                val stealIfOlderThan: Instant?,
            ) : TypedUpdate(
                """
                INSERT INTO ufw__database__locks AS l (id, owner, acquired_at) VALUES (:id, :owner, :now)
                ON CONFLICT (id) DO UPDATE 
                SET owner = :owner,
                    acquired_at = :now
                WHERE l.owner = :owner 
                   OR l.owner IS NULL
                   OR ((:stealIfOlderThan::timestamptz IS NOT NULL) AND l.acquired_at < :stealIfOlderThan)             
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            class Release(
                val id: String,
                val owner: String,
            ) : TypedUpdate(
                """
                UPDATE ufw__database__locks 
                SET owner = NULL,
                    acquired_at = NULL
                WHERE id = :id
                  AND owner = :owner     
                """.trimIndent()
            )

            object Truncate : TypedUpdate("DELETE FROM ufw__database__locks", minimumAffectedRows = 0)
        }
    }
}