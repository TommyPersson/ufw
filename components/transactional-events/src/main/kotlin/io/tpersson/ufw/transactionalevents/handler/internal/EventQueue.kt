package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.EventId
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent
import io.tpersson.ufw.transactionalevents.handler.internal.dao.EventEntityData
import java.time.Duration
import java.time.Instant

public interface EventQueue {
    public suspend fun enqueue(event: IncomingEvent, unitOfWork: UnitOfWork)

    public suspend fun pollOne(timeout: Duration): EventEntityData?

    public suspend fun markAsInProgress(eventId: EventId, watchdogId: String, unitOfWork: UnitOfWork)

    public suspend fun markAsSuccessful(eventId: EventId, watchdogId: String, unitOfWork: UnitOfWork)

    public suspend fun markAsFailed(eventId: EventId, error: Exception, watchdogId: String, unitOfWork: UnitOfWork)

    public suspend fun updateWatchdog(eventId: EventId, watchdogId: String): Boolean

    public suspend fun recordFailure(eventUid: Long, error: Exception, unitOfWork: UnitOfWork)

    public suspend fun rescheduleAt(eventId: EventId, at: Instant, watchdogId: String, unitOfWork: UnitOfWork)

    public suspend fun getNumberOfFailuresFor(eventUid: Long): Int
}

