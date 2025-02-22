package io.tpersson.ufw.jobqueue.v2.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.worker.WorkItemHandler
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler

public class DurableJobHandlerAdapter<TJob : Any>(
    private val jobDefinition: DurableJobDefinition<out TJob>,
    private val handler: DurableJobHandler<TJob>,
    private val objectMapper: ObjectMapper,
) : WorkItemHandler {

    public override suspend fun handle(item: WorkItemDbEntity) {
        if (item.type != jobDefinition.type) {
            error("Incorrect mapping detected: ${item.type}")
        }

        val jobJavaClass = jobDefinition.jobClass.java
        val job = objectMapper.readValue(item.dataJson, jobJavaClass)

        try {
            handler.handle(job)
        } catch (e: Exception) {
            println("oh no: $e")
        }
    }
}