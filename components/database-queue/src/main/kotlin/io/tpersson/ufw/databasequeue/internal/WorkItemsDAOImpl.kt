package io.tpersson.ufw.databasequeue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.typedqueries.TypedUpdateReturningSingle
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.WorkItemState
import jakarta.inject.Inject
import jakarta.inject.Named
import java.time.Instant

public class WorkItemsDAOImpl @Inject constructor(
    private val database: Database,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper
) : WorkItemsDAO {

    override suspend fun scheduleNewItem(newItem: NewWorkItem, now: Instant, unitOfWork: UnitOfWork) {
        val item = WorkItemDbEntity(
            uid = 0,
            itemId = newItem.itemId,
            queueId = newItem.queueId,
            type = newItem.type,
            state = WorkItemState.SCHEDULED.dbOrdinal,
            dataJson = newItem.dataJson,
            metadataJson = newItem.metadataJson,
            concurrencyKey = newItem.concurrencyKey,
            createdAt = now,
            firstScheduledFor = newItem.scheduleFor,
            nextScheduledFor = newItem.scheduleFor,
            stateChangedAt = now,
            watchdogTimestamp = null,
            watchdogOwner = null,
            numFailures = 0,
            expiresAt = null,
        )

        unitOfWork.add(
            Queries.Updates.InsertItem(
                item = item,
                eventJson = """[{ "@type": "SCHEDULED", "timestamp": "${item.createdAt}", "scheduledFor": "${item.nextScheduledFor}" }]"""
            )
        )
    }

    override suspend fun getById(queueId: String, itemId: String): WorkItemDbEntity? {
        return database.select(Queries.Selects.FindById(queueId, itemId))
    }

    override suspend fun listAllItems(): List<WorkItemDbEntity> {
        return database.select(Queries.Selects.ListAllItems(limit = 0))
    }

    override suspend fun takeNext(queueId: String, watchdogId: String, now: Instant): WorkItemDbEntity? {
        return database.update(
            Queries.Updates.TakeNext(
                queueId = queueId,
                watchdogOwner = watchdogId,
                now = now,
                eventJson = """{ "@type": "TAKEN", "timestamp": "$now" }"""
            )
        )
    }

    override suspend fun markInProgressItemAsSuccessful(
        queueId: String,
        itemId: String,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkInProgressItemAsSuccessful(
                queueId = queueId,
                itemId = itemId,
                expiresAt = expiresAt,
                watchdogOwner = watchdogId,
                now = now,
                eventJson = """{ "@type": "SUCCESSFUL", "timestamp": "$now" }"""
            )
        )
    }

    override suspend fun markInProgressItemAsFailed(
        queueId: String,
        itemId: String,
        expiresAt: Instant,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkInProgressItemAsFailed(
                queueId = queueId,
                itemId = itemId,
                expiresAt = expiresAt,
                watchdogOwner = watchdogId,
                now = now,
                eventJson = """{ "@type": "FAILED", "timestamp": "$now" }"""
            )
        )
    }

    override suspend fun rescheduleInProgressItem(
        queueId: String,
        itemId: String,
        watchdogId: String,
        scheduleFor: Instant,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.RescheduleInProgressItem(
                queueId = queueId,
                itemId = itemId,
                scheduleFor = scheduleFor,
                watchdogOwner = watchdogId,
                now = now,
                eventJson = """
                    [
                        { "@type": "FAILED", "timestamp": "$now" },
                        { "@type": "AUTOMATICALLY_RESCHEDULED", "timestamp": "$now", "scheduledFor": "$scheduleFor" }
                    ]
                    """.trimIndent()
            )
        )
    }

    override suspend fun manuallyRescheduleFailedItem(
        queueId: String,
        itemId: String,
        scheduleFor: Instant,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.RescheduleFailedItem(
                queueId = queueId,
                itemId = itemId,
                scheduleFor = scheduleFor,
                now = now,
                eventJson = """{ "@type": "MANUALLY_RESCHEDULED", "timestamp": "$now", "scheduledFor": "$scheduleFor" }"""
            )
        )
    }

    override suspend fun forceCancelItem(
        queueId: String,
        itemId: String,
        expireAt: Instant,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.ForceCancelItem(
                queueId = queueId,
                itemId = itemId,
                expireAt = expireAt,
                now = now,
                eventJson = """{ "@type": "CANCELLED", "timestamp": "$now" }"""
            )
        )
    }

    override suspend fun forcePauseItem(queueId: String, itemId: String, now: Instant, unitOfWork: UnitOfWork) {
        TODO("Not yet implemented")
    }

    override suspend fun pauseQueue(queueId: String, now: Instant, unitOfWork: UnitOfWork) {
        TODO("Not yet implemented")
    }

    override suspend fun refreshWatchdog(
        queueId: String,
        itemId: String,
        watchdogId: String,
        now: Instant,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.RefreshWatchdog(
                queueId = queueId,
                itemId = itemId,
                watchdogId = watchdogId,
                now = now
            )
        )
    }

    override suspend fun getEventsForItem(queueId: String, itemId: String): List<WorkItemEvent> {
        return database.select(Queries.Selects.GetEventsForItem(queueId, itemId))
            .map { objectMapper.readValue<WorkItemEvent>(it.event) }
    }

    override suspend fun getQueueStatistics(queueId: String): WorkItemQueueStatistics {
        val data = database.select(Queries.Selects.GetStatistics(queueId))

        val map = data.associateBy { WorkItemState.fromDbOrdinal(it.stateId) }

        return WorkItemQueueStatistics(
            queueId = queueId,
            numScheduled = map[WorkItemState.SCHEDULED]?.count ?: 0,
            numInProgress = map[WorkItemState.IN_PROGRESS]?.count ?: 0,
            numSuccessful = map[WorkItemState.SUCCESSFUL]?.count ?: 0,
            numFailed = map[WorkItemState.FAILED]?.count ?: 0,
            numPending = 0 // TODO pending = state == scheduled + next_scheduled_time <= now
        )
    }

    override suspend fun deleteExpiredItems(now: Instant): Int {
        return database.update(Queries.Updates.DeleteExpiredItems(now))
    }

    override suspend fun debugInsert(item: WorkItemDbEntity, unitOfWork: UnitOfWork?) {
        if (unitOfWork != null) {
            unitOfWork.add(
                Queries.Updates.InsertItem(
                    item = item,
                    eventJson = "[]"
                )
            )
        } else {
            database.update(
                Queries.Updates.InsertItem(
                    item = item,
                    eventJson = "[]"
                )
            )
        }
    }

    override suspend fun debugTruncate() {
        database.update(Queries.Updates.Truncate)
    }

    internal object Queries {
        const val TableName = "ufw__db_queue__items"

        val columnsWithoutEvents = listOf(
            "uid",
            "item_id",
            "queue_id",
            "type",
            "state",
            "data_json",
            "metadata_json",
            "concurrency_key",
            "created_at",
            "first_scheduled_for",
            "next_scheduled_for",
            "state_changed_at",
            "watchdog_timestamp",
            "watchdog_owner",
            "num_failures",
            "expires_at",
        )

        val columnsWithoutEventsSql = columnsWithoutEvents.joinToString(", ")

        object Selects {
            data class FindById(
                val queueId: String,
                val itemId: String
            ) : TypedSelectSingle<WorkItemDbEntity>(
                """
                SELECT $columnsWithoutEventsSql 
                FROM $TableName
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                """.trimIndent()
            )

            data class ListAllItems(val limit: Int) : TypedSelectList<WorkItemDbEntity>(
                """
                SELECT $columnsWithoutEventsSql  
                FROM $TableName
                ORDER BY created_at ASC
                """.trimIndent()
            )

            data class GetEventsForItem(
                val queueId: String,
                val itemId: String,
            ) : TypedSelectList<WorkItemEventWrapperDbEntity>(
                """
                SELECT jsonb_array_elements(events) as event
                FROM $TableName
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                """.trimIndent()
            )

            data class GetStatistics(
                val queueId: String
            ) : TypedSelectList<StatisticsData>(
                """
                SELECT count(*) as count, state as state_id
                FROM $TableName
                WHERE queue_id = :queueId
                GROUP BY state
                """.trimIndent()
            )
        }

        object Updates {
            data class InsertItem(
                val item: WorkItemDbEntity,
                val eventJson: String
            ) : TypedUpdate(
                """
                INSERT INTO $TableName (
                    item_id,
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
                    expires_at,
                    num_failures,
                    events
                ) VALUES (                    
                    :item.itemId,
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
                    :item.expiresAt,
                    :item.numFailures,
                    :eventJson::jsonb
                ) ON CONFLICT (queue_id, item_id) DO NOTHING
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class TakeNext(
                val queueId: String,
                val watchdogOwner: String,
                val now: Instant,
                val eventJson: String
            ) : TypedUpdateReturningSingle<WorkItemDbEntity>(
                """
                WITH in_progress_jobs AS (
                    SELECT uid, concurrency_key
                    FROM $TableName
                    WHERE state = ${WorkItemState.IN_PROGRESS.dbOrdinal}
                      AND queue_id = :queueId
                ), next_job AS (
                    SELECT uid 
                    FROM $TableName nj
                    WHERE state = ${WorkItemState.SCHEDULED.dbOrdinal}
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
                   state = ${WorkItemState.IN_PROGRESS.dbOrdinal},
                   state_changed_at = :now,
                   watchdog_timestamp = :now,
                   watchdog_owner = :watchdogOwner,
                   expires_at = NULL,
                   events = events || :eventJson::jsonb
                FROM next_job
                WHERE queue.uid = next_job.uid
                RETURNING ${columnsWithoutEvents.joinToString(",") { "queue.$it" }} 
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class MarkInProgressItemAsSuccessful(
                val queueId: String,
                val itemId: String,
                val expiresAt: Instant,
                val watchdogOwner: String,
                val now: Instant,
                val eventJson: String
            ) : TypedUpdate(
                """
                UPDATE $TableName SET
                   state = ${WorkItemState.SUCCESSFUL.dbOrdinal},
                   state_changed_at = :now,
                   next_scheduled_for = NULL,
                   watchdog_timestamp = NULL,
                   watchdog_owner = NULL,
                   expires_at = :expiresAt,
                   events = events || :eventJson::jsonb
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                  AND watchdog_owner = :watchdogOwner
                  AND state = ${WorkItemState.IN_PROGRESS.dbOrdinal}                     
                """.trimIndent(),
                minimumAffectedRows = 1
            )

            data class MarkInProgressItemAsFailed(
                val queueId: String,
                val itemId: String,
                val expiresAt: Instant,
                val watchdogOwner: String,
                val now: Instant,
                val eventJson: String
            ) : TypedUpdate(
                """
                UPDATE $TableName SET
                   state = ${WorkItemState.FAILED.dbOrdinal},
                   state_changed_at = :now,
                   next_scheduled_for = NULL,
                   watchdog_timestamp = NULL,
                   watchdog_owner = NULL,
                   expires_at = :expiresAt,
                   num_failures = num_failures + 1,
                   events = events || :eventJson::jsonb   
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                  AND watchdog_owner = :watchdogOwner
                  AND state = ${WorkItemState.IN_PROGRESS.dbOrdinal}             
                """.trimIndent(),
                minimumAffectedRows = 1
            )

            data class RescheduleInProgressItem(
                val queueId: String,
                val itemId: String,
                val scheduleFor: Instant,
                val watchdogOwner: String,
                val now: Instant,
                val eventJson: String
            ) : TypedUpdate(
                """
                UPDATE $TableName SET
                   state = ${WorkItemState.SCHEDULED.dbOrdinal},
                   state_changed_at = :now,
                   next_scheduled_for = :scheduleFor,
                   watchdog_timestamp = NULL,
                   watchdog_owner = NULL,
                   expires_at = NULL,
                   num_failures = num_failures + 1,
                   events = events || :eventJson::jsonb    
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                  AND watchdog_owner = :watchdogOwner
                  AND state = ${WorkItemState.IN_PROGRESS.dbOrdinal}                 
                """.trimIndent(),
                minimumAffectedRows = 1
            )

            data class RescheduleFailedItem(
                val queueId: String,
                val itemId: String,
                val scheduleFor: Instant,
                val now: Instant,
                val eventJson: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName SET
                   state = ${WorkItemState.SCHEDULED.dbOrdinal},
                   state_changed_at = :now,
                   next_scheduled_for = :scheduleFor,
                   expires_at = NULL,
                   events = events || :eventJson::jsonb    
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                  AND state = ${WorkItemState.FAILED.dbOrdinal}
                """.trimIndent(),
                minimumAffectedRows = 1
            )

            data class ForceCancelItem(
                val queueId: String,
                val itemId: String,
                val expireAt: Instant,
                val now: Instant,
                val eventJson: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName SET
                   state = ${WorkItemState.CANCELLED.dbOrdinal},
                   state_changed_at = :now,
                   next_scheduled_for = null,
                   expires_at = :expireAt,
                   watchdog_owner = NULL,
                   watchdog_timestamp = NULL,
                   events = events || :eventJson::jsonb
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                """.trimIndent(),
                minimumAffectedRows = 1
            )

            data class RefreshWatchdog(
                val queueId: String,
                val itemId: String,
                val watchdogId: String,
                val now: Instant,
            ) : TypedUpdate(
                """
                UPDATE $TableName SET
                   watchdog_timestamp = :now
                WHERE queue_id = :queueId
                  AND item_id = :itemId
                  AND watchdog_owner = :watchdogId
                """.trimIndent(),
                minimumAffectedRows = 1
            )

            data class DeleteExpiredItems(
                val now: Instant,
            ) : TypedUpdate(
                """
                DELETE FROM $TableName 
                WHERE expires_at <= :now
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

    internal class StatisticsData(
        val count: Int,
        val stateId: Int
    )
}