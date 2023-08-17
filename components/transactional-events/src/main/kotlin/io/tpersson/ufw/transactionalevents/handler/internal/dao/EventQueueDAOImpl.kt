package io.tpersson.ufw.transactionalevents.handler.internal.dao

import io.tpersson.ufw.database.exceptions.TypedUpdateMinimumAffectedRowsException
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.handler.EventQueueId
import io.tpersson.ufw.transactionalevents.handler.EventState
import io.tpersson.ufw.transactionalevents.handler.internal.exceptions.EventOwnershipLostException
import io.tpersson.ufw.transactionalevents.handler.internal.metrics.EventQueueStatistics
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Instant

@Singleton
public class EventQueueDAOImpl @Inject constructor(
    private val database: Database
) : EventQueueDAO {
    override suspend fun insert(event: EventEntityData, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.Insert(event))
    }

    override suspend fun getNext(queueId: EventQueueId, now: Instant): EventEntityData? {
        return database.select(Queries.Selects.SelectNextEvent(queueId, now))
    }

    override suspend fun getById(queueId: EventQueueId, eventId: EventId): EventEntityData? {
        return database.select(Queries.Selects.GetById(queueId, eventId))
    }

    override suspend fun markAsInProgress(
        queueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkEventAsInProgress(
                queueId = queueId,
                eventId = eventId,
                timestamp = now,
                watchdogOwner = watchdogId,
            ),
        )
    }

    override suspend fun markAsSuccessful(
        queueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkEventAsSuccessful(
                queueId = queueId,
                eventId = eventId,
                timestamp = now,
                expireAt = expireAt,
                expectedWatchdogOwner = watchdogId
            ),
            ::exceptionMapper
        )
    }

    override suspend fun markAsFailed(
        queueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkEventAsFailed(
                queueId = queueId,
                eventId = eventId,
                timestamp = now,
                expireAt = expireAt,
                expectedWatchdogOwner = watchdogId
            ),
            ::exceptionMapper
        )
    }

    override suspend fun markAsScheduled(
        queueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        scheduleFor: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkEventAsScheduled(
                queueId = queueId,
                eventId = eventId,
                timestamp = now,
                scheduledFor = scheduleFor,
                expectedWatchdogOwner = watchdogId
            ),
            ::exceptionMapper
        )
    }

    override suspend fun markStaleEventsAsScheduled(
        now: Instant,
        staleIfWatchdogOlderThan: Instant
    ): Int {
        return database.update(
            Queries.Updates.MarkStaleEventsAsScheduled(
                timestamp = now,
                staleIfWatchdogOlderThan = staleIfWatchdogOlderThan
            )
        )
    }

    override suspend fun updateWatchdog(
        eventUid: Long,
        now: Instant,
        watchdogId: String
    ): Boolean {
        val affectedRows = database.update(Queries.Updates.UpdateWatchdog(
            eventUid = eventUid,
            watchdogTimestamp = now,
            expectedWatchdogOwner = watchdogId
        ))

        return affectedRows > 0
    }

    override suspend fun deleteExpiredEvents(now: Instant): Int {
        return database.update(Queries.Updates.DeleteExpiredEvents(now))
    }

    override suspend fun getQueueStatistics(queueId: EventQueueId): EventQueueStatistics {
        val data = database.select(Queries.Selects.GetStatistics(queueId.id))

        val map = data.associateBy { EventState.fromId(it.stateId) }

        return EventQueueStatistics(
            queueId = queueId,
            numScheduled = map[EventState.Scheduled]?.count ?: 0,
            numInProgress = map[EventState.InProgress]?.count ?: 0,
            numSuccessful = map[EventState.Successful]?.count ?: 0,
            numFailed = map[EventState.Failed]?.count ?: 0,
        )
    }

    override suspend fun debugGetAllEvents(queueId: EventQueueId?): List<EventEntityData> {
        return database.select(Queries.Selects.DebugSelectAll(queueId))
    }

    override suspend fun debugTruncate() {
        database.update(Queries.Updates.DebugTruncate)
    }

    private fun exceptionMapper(exception: Exception): Exception {
        throw when (exception) {
            is TypedUpdateMinimumAffectedRowsException -> {
                if (exception.query::class in Queries.Updates.queriesThatRequireOwnership) {
                    EventOwnershipLostException(exception)
                } else exception
            }

            else -> exception
        }
    }

    internal object Queries {
        const val TableName = "ufw__transactional_events__queue"

        object Selects {
            data class SelectNextEvent(
                val queueId: EventQueueId,
                val now: Instant,
            ) : TypedSelectSingle<EventEntityData>(
                """
                SELECT * 
                FROM $TableName
                WHERE state = ${EventState.Scheduled.id}
                  AND queue_id = :queueId.id
                  AND scheduled_for <= :now
                ORDER BY scheduled_for ASC
                LIMIT 1
                """.trimIndent()
            )

            data class GetById(
                val queueId: EventQueueId,
                val eventId: EventId,
            ) : TypedSelectSingle<EventEntityData>(
                """
                SELECT * 
                FROM $TableName
                WHERE queue_id = :queueId.id
                  AND id = :eventId.value
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

            data class DebugSelectAll(
                val queueId: EventQueueId?
            ) : TypedSelectList<EventEntityData>(
                """
                SELECT * FROM $TableName 
                WHERE queue_id = :queueId.id::text
                   OR :queueId.id::text IS NULL
                """
            )
        }

        object Updates {
            val queriesThatRequireOwnership = listOf(
                MarkEventAsSuccessful::class,
                MarkEventAsFailed::class,
                MarkEventAsScheduled::class,
                UpdateWatchdog::class
            )

            class Insert(val data: EventEntityData) : TypedUpdate(
                """
                INSERT INTO $TableName (
                    queue_id, 
                    id, 
                    topic, 
                    type, 
                    data_json, 
                    ce_data_json, 
                    timestamp,
                    state,
                    created_at, 
                    scheduled_for, 
                    state_changed_at, 
                    watchdog_timestamp, 
                    watchdog_owner, 
                    expire_at
                ) VALUES (
                    :data.queueId, 
                    :data.id, 
                    :data.topic, 
                    :data.type, 
                    :data.dataJson::jsonb, 
                    :data.ceDataJson::jsonb, 
                    :data.timestamp, 
                    :data.state,
                    :data.createdAt, 
                    :data.scheduledFor, 
                    :data.stateChangedAt, 
                    :data.watchdogTimestamp, 
                    :data.watchdogOwner, 
                    :data.expireAt
                ) ON CONFLICT (queue_id, id) DO NOTHING 
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class MarkEventAsInProgress(
                val queueId: EventQueueId,
                val eventId: EventId,
                val timestamp: Instant,
                val toState: Int = EventState.InProgress.id,
                val watchdogOwner: String
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    watchdog_timestamp = :timestamp,
                    watchdog_owner = :watchdogOwner
                WHERE queue_id = :queueId.id
                  AND id = :eventId.value
                  AND state = ${EventState.Scheduled.id}
                """.trimIndent()
            )

            data class MarkEventAsSuccessful(
                val eventId: EventId,
                val queueId: EventQueueId,
                val timestamp: Instant,
                val expireAt: Instant,
                val toState: Int = EventState.Successful.id,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    expire_at = :expireAt,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${EventState.InProgress.id}
                  AND id = :eventId.value
                  AND queue_id = :queueId.id
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent()
            )

            data class MarkEventAsFailed(
                val eventId: EventId,
                val queueId: EventQueueId,
                val timestamp: Instant,
                val expireAt: Instant,
                val toState: Int = EventState.Failed.id,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    expire_at = :expireAt,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${EventState.InProgress.id}
                  AND queue_id = :queueId.id
                  AND id = :eventId.value
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent()
            )

            data class MarkEventAsScheduled(
                val eventId: EventId,
                val queueId: EventQueueId,
                val timestamp: Instant,
                val scheduledFor: Instant,
                val toState: Int = EventState.Scheduled.id,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    scheduled_for = :scheduledFor,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${EventState.InProgress.id}
                  AND queue_id = :queueId.id
                  AND id = :eventId.value
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent()
            )

            data class MarkStaleEventsAsScheduled(
                val timestamp: Instant,
                val staleIfWatchdogOlderThan: Instant,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = ${EventState.Scheduled.id},
                    state_changed_at = :timestamp,
                    scheduled_for = :timestamp,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${EventState.InProgress.id}
                  AND watchdog_timestamp < :staleIfWatchdogOlderThan
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class UpdateWatchdog(
                val eventUid: Long,
                val watchdogTimestamp: Instant,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET watchdog_timestamp = :watchdogTimestamp
                WHERE uid = :eventUid
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class DeleteExpiredEvents(
                val now: Instant,
            ) : TypedUpdate(
                """
                DELETE FROM $TableName
                WHERE expire_at < :now
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            object DebugTruncate : TypedUpdate(
                "DELETE FROM $TableName",
                minimumAffectedRows = 0
            )
        }
    }

    internal class StatisticsData(
        val count: Int,
        val stateId: Int
    )
}

