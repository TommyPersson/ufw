package io.tpersson.ufw.jobqueue.v2.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.databasequeue.FailureAction
import io.tpersson.ufw.databasequeue.WorkItemFailureContext
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import io.tpersson.ufw.jobqueue.v2.JobFailureContext
import java.time.Instant
import java.time.InstantSource

public class DurableJobHandlerAdapter<TJob : Any>(
    private val jobDefinition: DurableJobDefinition<out TJob>,
    private val handler: DurableJobHandler<TJob>,
    private val objectMapper: ObjectMapper,
) : WorkItemHandler<TJob> {

    public override suspend fun handle(item: TJob) {
        handler.handle(item)
    }

    override fun transformItem(rawItem: WorkItemDbEntity): TJob {
        if (rawItem.type != jobDefinition.type) {
            error("Incorrect mapping detected: ${rawItem.type}")
        }

        val jobJavaClass = jobDefinition.jobClass.java

        return objectMapper.readValue(rawItem.dataJson, jobJavaClass)
    }

    override suspend fun onFailure(item: TJob, error: Exception, context: WorkItemFailureContext): FailureAction {
        val jobFailureContext = object : JobFailureContext {
            override val clock: InstantSource = context.clock
            override val timestamp: Instant = context.timestamp
            override val failureCount: Int = context.failureCount
        }

        return handler.onFailure(item, error, jobFailureContext)
    }
}