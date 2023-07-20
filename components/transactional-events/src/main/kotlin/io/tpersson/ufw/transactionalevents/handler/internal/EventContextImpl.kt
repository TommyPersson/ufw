package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.handler.EventContext

public class EventContextImpl(
    override val unitOfWork: UnitOfWork
) : EventContext