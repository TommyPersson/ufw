package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.typedqueries.TypedUpdateReturningSingle
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject
import java.time.Instant

public class WorkItemsDAOImpl @Inject constructor(
    private val database: Database
) : WorkItemsDAO {

    override suspend fun insert(item: WorkItemDbEntity, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.InsertItem(item))
    }

    override suspend fun getById(id: String): WorkItemDbEntity? {
        return database.select(Queries.Selects.FindById(id))
    }

    override suspend fun listAllItems(): List<WorkItemDbEntity> {
        return database.select(Queries.Selects.ListAllItems(limit = 0))
    }

    override suspend fun takeNext(queueId: String, watchdogId: String, now: Instant): WorkItemDbEntity? {
        return database.update(Queries.Updates.TakeNext(queueId, watchdogId, now))
    }

    override suspend fun debugTruncate() {
        database.update(Queries.Updates.Truncate)
    }

    internal object Queries {
        val TableName = "ufw__db_queue__items"

        object Selects {
            data class FindById(val id: String) : TypedSelectSingle<WorkItemDbEntity>(
                """
                SELECT * 
                FROM $TableName
                WHERE id = :id
                """.trimIndent()
            )

            data class ListAllItems(val limit: Int) : TypedSelectList<WorkItemDbEntity>(
                """
                SELECT * 
                FROM $TableName
                ORDER BY created_at ASC
                """.trimIndent()
            )
        }

        object Updates {
            data class InsertItem(val item: WorkItemDbEntity) : TypedUpdate(
                """
                INSERT INTO $TableName (
                    id,
                    queue_id,
                    type,
                    state,
                    data_json,
                    metadata_json,
                    concurrency_key,
                    created_at,
                    first_scheduled_for,
                    next_scheduled_for,
                    state_changed_at,
                    watchdog_timestamp,
                    watchdog_owner,
                    expires_at
                ) VALUES (                    
                    :item.id,
                    :item.queueId,
                    :item.type,
                    :item.state,
                    :item.dataJson,
                    :item.metadataJson,
                    :item.concurrencyKey,
                    :item.createdAt,
                    :item.firstScheduledFor,
                    :item.nextScheduledFor,
                    :item.stateChangedAt,
                    :item.watchdogTimestamp,
                    :item.watchdogOwner,
                    :item.expiresAt
                ) ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class TakeNext(
                val queueId: String,
                val watchdogOwner: String,
                val now: Instant
            ) : TypedUpdateReturningSingle<WorkItemDbEntity>(
                """
                WITH in_progress_jobs AS (
                    SELECT uid, concurrency_key
                    FROM $TableName
                    WHERE state = ${WorkItemState.IN_PROGRESS}
                      AND queue_id = :queueId
                ), next_job AS (
                    SELECT uid 
                    FROM $TableName nj
                    WHERE state = ${WorkItemState.SCHEDULED}
                      AND (nj.concurrency_key IS NULL OR 0 = (
                        SELECT count(ipj.*)
                        FROM in_progress_jobs ipj
                        WHERE ipj.concurrency_key = nj.concurrency_key 
                      ))
                    ORDER BY next_scheduled_for ASC
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED 
                )   
                UPDATE $TableName queue SET
                   state = ${WorkItemState.IN_PROGRESS},
                   state_changed_at = :now,
                   watchdog_timestamp = :now,
                   watchdog_owner = :watchdogOwner,
                   expires_at = NULL
                FROM next_job
                WHERE queue.uid = next_job.uid
                RETURNING *
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            object Truncate : TypedUpdate(
                """
                DELETE FROM $TableName
                """.trimIndent(),
                minimumAffectedRows = 0
            )
        }
    }
}