package io.tpersson.ufw.durablemessages.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.utils.LoggerCache
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.WorkItemContext
import io.tpersson.ufw.databasequeue.WorkItemFailureContext
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.handler.DurableMessageContext
import io.tpersson.ufw.durablemessages.handler.DurableMessageFailureContext
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import org.slf4j.Logger
import java.time.Instant
import java.time.Clock

public class DurableMessageHandlerAdapter<TMessage : DurableMessage>(
    private val handler: DurableMessageHandler,
    private val method: DurableMessageHandlerMethod<TMessage>,
    private val objectMapper: ObjectMapper,
) : WorkItemHandler<TMessage> {

    override val handlerClassName: String = handler::class.simpleName!!

    override val logger: Logger = LoggerCache.get(handler::class)

    override fun transformItem(rawItem: WorkItemDbEntity): TMessage {
        return objectMapper.readValue(rawItem.dataJson, method.messageClass.java)
    }

    override suspend fun handle(item: TMessage, context: WorkItemContext) {
        method.method.invoke(item, context.asDurableMessageContext())
    }

    override suspend fun onFailure(item: TMessage, error: Exception, context: WorkItemFailureContext): FailureAction {
        return method.handler.onFailure(item, error, context.asDurableMessageFailureContext())
    }
}

// TODO real Impl to avoid class gc
public fun WorkItemContext.asDurableMessageContext(): DurableMessageContext =
    object : DurableMessageContext {
        override val clock: Clock = this@asDurableMessageContext.clock
        override val timestamp: Instant = this@asDurableMessageContext.timestamp
        override val failureCount: Int = this@asDurableMessageContext.failureCount
        override val unitOfWork: UnitOfWork = this@asDurableMessageContext.unitOfWork
        override val logger: Logger = this@asDurableMessageContext.logger
    }

// TODO real Impl to avoid class gc
public fun WorkItemFailureContext.asDurableMessageFailureContext(): DurableMessageFailureContext =
    object : DurableMessageFailureContext {
        override val clock: Clock = this@asDurableMessageFailureContext.clock
        override val timestamp: Instant = this@asDurableMessageFailureContext.timestamp
        override val failureCount: Int = this@asDurableMessageFailureContext.failureCount
        override val unitOfWork: UnitOfWork = this@asDurableMessageFailureContext.unitOfWork
        override val logger: Logger = this@asDurableMessageFailureContext.logger
    }