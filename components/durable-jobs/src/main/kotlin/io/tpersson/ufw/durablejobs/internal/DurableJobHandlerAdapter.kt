package io.tpersson.ufw.durablejobs.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.WorkItemContext
import io.tpersson.ufw.databasequeue.WorkItemFailureContext
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.durablejobs.DurableJob
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.durablejobs.DurableJobContext
import io.tpersson.ufw.durablejobs.DurableJobFailureContext
import java.time.Instant
import java.time.Clock

public class DurableJobHandlerAdapter<TJob : DurableJob>(
    private val jobDefinition: DurableJobDefinition<out TJob>,
    private val handler: DurableJobHandler<TJob>,
    private val objectMapper: ObjectMapper,
) : WorkItemHandler<TJob> {

    override val handlerClassName: String = handler::class.simpleName!!

    public override suspend fun handle(item: TJob, context: WorkItemContext) {
        val jobContext = object : DurableJobContext {
            override val clock: Clock = context.clock
            override val timestamp: Instant = context.timestamp
            override val failureCount: Int = context.failureCount
            override val unitOfWork: UnitOfWork = context.unitOfWork
        }

        handler.handle(item, jobContext)
    }

    override fun transformItem(rawItem: WorkItemDbEntity): TJob {
        if (rawItem.type != jobDefinition.type) {
            error("Incorrect mapping detected: ${rawItem.type}")
        }

        val jobJavaClass = jobDefinition.jobClass.java

        return objectMapper.readValue(rawItem.dataJson, jobJavaClass)
    }

    override suspend fun onFailure(item: TJob, error: Exception, context: WorkItemFailureContext): FailureAction {
        val jobFailureContext = object : DurableJobFailureContext {
            override val clock: Clock = context.clock
            override val timestamp: Instant = context.timestamp
            override val failureCount: Int = context.failureCount
            override val unitOfWork: UnitOfWork = context.unitOfWork
        }

        return handler.onFailure(item, error, jobFailureContext)
    }
}