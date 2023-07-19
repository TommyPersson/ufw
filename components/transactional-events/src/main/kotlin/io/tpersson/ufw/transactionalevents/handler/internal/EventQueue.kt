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

    public suspend fun markAsInProgress(id: EventId, watchdogId: String, uow: UnitOfWork)

    public suspend fun updateWatchdog(id: EventId, watchdogId: String): Boolean

    public suspend fun markAsSuccessful(id: EventId, watchdogId: String, uow: UnitOfWork)
}

