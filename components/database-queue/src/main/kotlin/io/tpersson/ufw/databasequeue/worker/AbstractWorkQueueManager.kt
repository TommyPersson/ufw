package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.managed.Managed
import kotlinx.coroutines.*

public abstract class AbstractWorkQueueManager(
    private val workerFactory: DatabaseQueueWorkerFactory,
    private val adapterSettings: DatabaseQueueAdapterSettings,
) : Managed() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var coroutine: Job? = null

    override suspend fun onStarted() {
        coroutine = startAll()
    }

    override suspend fun onStopped() {
        coroutine?.cancelAndJoin()
    }

    public fun startAll(): Job {
        return scope.launch {
            handlersByTypeByQueueId.forEach { (queueId, handlersByType) ->
                val fullQueueId = adapterSettings.queueIdPrefix + queueId
                val worker = workerFactory.create(fullQueueId, handlersByType, adapterSettings)
                launch {
                    worker.start()
                }
            }
        }
    }

    protected abstract val handlersByTypeByQueueId: Map<String, Map<String, WorkItemHandler<*>>>
}