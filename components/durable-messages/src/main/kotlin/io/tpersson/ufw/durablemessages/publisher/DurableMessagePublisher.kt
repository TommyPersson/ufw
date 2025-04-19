package io.tpersson.ufw.durablemessages.publisher

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablemessages.common.DurableMessage

public interface DurableMessagePublisher {
    public suspend fun publish(message: DurableMessage, unitOfWork: UnitOfWork)
    public suspend fun publishAll(messages: List<DurableMessage>, unitOfWork: UnitOfWork)
}
