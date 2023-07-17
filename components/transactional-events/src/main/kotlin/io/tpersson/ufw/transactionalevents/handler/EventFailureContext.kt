package io.tpersson.ufw.transactionalevents.handler

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface EventFailureContext {
    public val numberOfFailures: Int
    public val unitOfWork: UnitOfWork
}