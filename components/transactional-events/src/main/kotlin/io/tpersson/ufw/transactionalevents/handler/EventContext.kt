package io.tpersson.ufw.transactionalevents.handler

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface EventContext {
    public val unitOfWork: UnitOfWork
}