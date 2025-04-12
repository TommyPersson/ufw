package io.tpersson.ufw.durableevents.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.WorkItemContext
import io.tpersson.ufw.databasequeue.WorkItemFailureContext
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.durableevents.common.DurableEvent
import io.tpersson.ufw.durableevents.handler.DurableEventContext
import io.tpersson.ufw.durableevents.handler.DurableEventFailureContext
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import java.time.Instant
import java.time.Clock

public class DurableEventHandlerAdapter<TEvent : DurableEvent>(
    private val handler: DurableEventHandler,
    private val method: DurableEventHandlerMethod<TEvent>,
    private val objectMapper: ObjectMapper,
) : WorkItemHandler<TEvent> {

    override val handlerClassName: String = handler::class.simpleName!!

    override fun transformItem(rawItem: WorkItemDbEntity): TEvent {
        return objectMapper.readValue(rawItem.dataJson, method.eventClass.java)
    }

    override suspend fun handle(item: TEvent, context: WorkItemContext) {
        method.method.invoke(item, context.asDurableEventContext())
    }

    override suspend fun onFailure(item: TEvent, error: Exception, context: WorkItemFailureContext): FailureAction {
        return method.handler.onFailure(item, error, context.asDurableEventFailureContext())
    }
}

// TODO real Impl to avoid class gc
public fun WorkItemContext.asDurableEventContext(): DurableEventContext = object : DurableEventContext {
    override val clock: Clock = this@asDurableEventContext.clock
    override val timestamp: Instant = this@asDurableEventContext.timestamp
    override val failureCount: Int = this@asDurableEventContext.failureCount
    override val unitOfWork: UnitOfWork = this@asDurableEventContext.unitOfWork
}

// TODO real Impl to avoid class gc
public fun WorkItemFailureContext.asDurableEventFailureContext(): DurableEventFailureContext = object  :
    DurableEventFailureContext {
    override val clock: Clock = this@asDurableEventFailureContext.clock
    override val timestamp: Instant = this@asDurableEventFailureContext.timestamp
    override val failureCount: Int = this@asDurableEventFailureContext.failureCount
    override val unitOfWork: UnitOfWork = this@asDurableEventFailureContext.unitOfWork
}