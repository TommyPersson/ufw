package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.handler.IncomingEvent

public interface EventQueue {
    public fun enqueue(event: IncomingEvent, unitOfWork: UnitOfWork)
}