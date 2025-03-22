package io.tpersson.ufw.durableevents.publisher

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durableevents.common.DurableEvent

public interface DurableEventPublisher {
    public fun publish(event: DurableEvent, unitOfWork: UnitOfWork)
    public fun publishAll(events: List<DurableEvent>, unitOfWork: UnitOfWork)
}
