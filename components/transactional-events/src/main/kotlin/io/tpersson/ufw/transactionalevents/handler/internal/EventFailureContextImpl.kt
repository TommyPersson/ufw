package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.handler.EventFailureContext

public class EventFailureContextImpl(
    override val numberOfFailures: Int,
    override val unitOfWork: UnitOfWork
) : EventFailureContext {
}