package io.tpersson.ufw.jobqueue.v2.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorker
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlinx.coroutines.Job

public class DurableJobQueueWorker @Inject constructor(
    private val queueId: String,
    private val workerFactory: DatabaseQueueWorkerFactory,
    private val handlersByType: Map<String, DurableJobHandler<*>>,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper
) {
    @Suppress("UNCHECKED_CAST")
    private val adaptersByType = handlersByType.mapValues {
        DurableJobHandlerAdapter(
            jobDefinition = it.value.jobDefinition,
            objectMapper = objectMapper,
            handler = it.value as DurableJobHandler<Any>,
        )
    }

    private val queueWorker = workerFactory.create(
        queueId = queueId,
        handlersByType = adaptersByType
        )

    public fun start(): Job {
        return queueWorker.start()
    }
}