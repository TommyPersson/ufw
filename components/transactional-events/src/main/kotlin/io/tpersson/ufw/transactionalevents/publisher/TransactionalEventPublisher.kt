package io.tpersson.ufw.transactionalevents.publisher

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.Event

public interface TransactionalEventPublisher {
    public fun publish(topic: String, event: Event, unitOfWork: UnitOfWork)
    public fun publishAll(topic: String, events: List<Event>, unitOfWork: UnitOfWork)
}
