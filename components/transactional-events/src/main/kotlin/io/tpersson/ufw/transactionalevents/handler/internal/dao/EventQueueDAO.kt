package io.tpersson.ufw.transactionalevents.handler.internal.dao

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.handler.EventQueueId
import io.tpersson.ufw.transactionalevents.handler.internal.metrics.EventQueueStatistics
import java.time.Instant

public interface EventQueueDAO {
    public suspend fun insert(event: EventEntityData, unitOfWork: UnitOfWork)

    public suspend fun takeNext(
        queueId: EventQueueId,
        now: Instant,
        watchdogId: String,
    ): EventEntityData?

    public suspend fun getById(
        queueId: EventQueueId,
        eventId: EventId
    ): EventEntityData?

    public suspend fun markAsSuccessful(
        queueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun markAsFailed(
        queueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun markAsScheduled(
        queueId: EventQueueId,
        eventId: EventId,
        now: Instant,
        scheduleFor: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    )

    public suspend fun markStaleEventsAsScheduled(
        now: Instant,
        staleIfWatchdogOlderThan: Instant
    ): Int

    public suspend fun updateWatchdog(
        eventUid: Long,
        now: Instant,
        watchdogId: String,
    ): Boolean

    public suspend fun deleteExpiredEvents(now: Instant): Int

    public suspend fun getQueueStatistics(queueId: EventQueueId): EventQueueStatistics

    public suspend fun debugGetAllEvents(queueId: EventQueueId? = null): List<EventEntityData>

    public suspend fun debugTruncate()
}
