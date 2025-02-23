package io.tpersson.ufw.jobqueue.v2.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.jobqueue.v2.DurableJobHandler
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlinx.coroutines.*


public class DurableJobQueueWorkersManager @Inject constructor(
    private val workerFactory: DatabaseQueueWorkerFactory,
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : Managed() {
    private val mappedHandlers = durableJobHandlersProvider.get().map { createTypeMapping(it) }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var coroutine: Job? = null

    override suspend fun onStarted() {
        coroutine = startAll()
    }

    override suspend fun onStopped() {
        coroutine?.cancelAndJoin()
    }

    public fun startAll(): Job {
        val allQueueIds = mappedHandlers.map { it.jobDefinition.queueId }.toSet()

        val workersByQueue = allQueueIds.map { queueId ->
            val handlersForQueue = mappedHandlers
                .filter { it.jobDefinition.queueId == queueId }
                .map { it.handler }

            val handlersByType = handlersForQueue.associateBy { it.jobDefinition.type }

            DurableJobQueueWorker(
                queueId = queueId,
                workerFactory = workerFactory,
                handlersByType = handlersByType,
                objectMapper = objectMapper
            )
        }

        return scope.launch {
            workersByQueue.forEach { worker ->
                launch {
                    worker.start()
                }
            }
        }
    }

    private fun createTypeMapping(handler: DurableJobHandler<out Any>): DurableJobTypeMapping<*> {
        return DurableJobTypeMapping(
            jobDefinition = handler.jobDefinition,
            handler = handler,
        )
    }

}