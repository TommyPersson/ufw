package io.tpersson.ufw.transactionalevents.handler.internal.dao

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.handler.EventQueueId
import io.tpersson.ufw.transactionalevents.handler.EventState
import jakarta.inject.Inject
import java.time.Instant

public class EventQueueDAOImpl @Inject constructor(
    private val database: Database
) : EventQueueDAO {
    override suspend fun insert(event: EventEntityData, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.Insert(event))
    }

    override suspend fun getNext(eventQueueId: EventQueueId, now: Instant): EventEntityData? {
        return database.select(Queries.Selects.SelectNextEvent(eventQueueId, now))
    }

    override suspend fun getById(eventQueueId: EventQueueId, eventId: EventId): EventEntityData? {
        TODO("Not yet implemented")
    }

    override suspend fun markAsInProgress(
        eventQueueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkEventAsInProgress(
                queueId = eventQueueId,
                eventId = eventId,
                timestamp = now,
                watchdogOwner = watchdogId,
            )
        )
    }

    override suspend fun markAsSuccessful(
        eventQueueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkEventAsSuccessful(
                queueId = eventQueueId,
                eventId = eventId,
                timestamp = now,
                expireAt = expireAt,
                expectedWatchdogOwner = watchdogId
            )
        )
    }

    override suspend fun markAsFailed(
        eventQueueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun markAsScheduled(
        eventQueueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        scheduleFor: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun markStaleEventsAsScheduled(
        now: Instant,
        staleIfWatchdogOlderThan: Instant
    ): Int {
        TODO("Not yet implemented")
    }

    override suspend fun updateWatchdog(
        eventQueueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        watchdogId: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteExpiredEvents(now: Instant): Int {
        TODO("Not yet implemented")
    }

    override suspend fun debugGetAllEvents(): List<EventEntityData> {
        TODO("Not yet implemented")
    }

    override suspend fun debugTruncate(unitOfWork: UnitOfWork) {
        TODO("Not yet implemented")
    }

    internal object Queries {
        const val TableName = "ufw__transactional_events__queue"

        object Selects {
            data class SelectNextEvent(
                val queueId: EventQueueId,
                val now: Instant,
            ) : TypedSelect<EventEntityData>(
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
        }

        object Updates {
            class Insert(val data: EventEntityData) : TypedUpdate(
                """
                INSERT INTO ufw__transactional_events__queue (
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
        }
    }
}
