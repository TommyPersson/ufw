package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkQueueState
import jakarta.inject.Inject
import java.time.Instant

public class WorkQueuesDAOImpl @Inject constructor(
    private val database: Database
) : WorkQueuesDAO {
    override suspend fun getWorkQueue(queueId: WorkItemQueueId): WorkQueueDbEntity? {
        return database.select(Queries.Selects.GetById(queueId = queueId.value))
    }

    override suspend fun setWorkQueueState(queueId: WorkItemQueueId, state: WorkQueueState, now: Instant) {
        database.update(Queries.Updates.SetState(queueId = queueId.value, state = state.name, now = now))
    }

    override suspend fun debugTruncate() {
        database.update(Queries.Updates.DebugTruncate)
    }

    internal object Queries {
        const val TableName = "ufw__db_queue__queues"

        object Selects {
            data class GetById(
                val queueId: String
            ) : TypedSelectSingle<WorkQueueDbEntity>(
                """
                SELECT * 
                FROM $TableName 
                WHERE queue_id = :queueId    
                """.trimIndent()
            )
        }

        object Updates {

            data class SetState(
                val queueId: String,
                val state: String,
                val now: Instant
            ) : TypedUpdate(
                """
                INSERT INTO $TableName AS t (
                  queue_id,
                  state,
                  state_changed_at
                ) VALUES (
                  :queueId,
                  :state,
                  :now  
                ) ON CONFLICT (queue_id) DO UPDATE 
                  SET state = :state,
                      state_changed_at = :now
                  WHERE t.queue_id = :queueId               
                """.trimIndent()
            )

            @Suppress("SqlWithoutWhere")
            object DebugTruncate : TypedUpdate(
                "DELETE FROM $TableName",
                minimumAffectedRows = 0
            )
        }
    }
}