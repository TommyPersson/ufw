package io.tpersson.ufw.transactionalevents.handler

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public class EventContextImpl(
    override val unitOfWork: UnitOfWork
) : EventContext