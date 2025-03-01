package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.databasequeue.DatabaseQueueMdcLabels
import io.tpersson.ufw.databasequeue.WorkItemHandler
import kotlinx.coroutines.*
import java.time.Duration
import java.util.*

public class DatabaseQueueWorker(
    private val queueId: String,
    private val handlersByType: Map<String, WorkItemHandler<*>>,
    processorFactory: SingleWorkItemProcessorFactory,
    mdcLabels: DatabaseQueueMdcLabels,
) {
    private val logger = createLogger()

    private val fallbackPollInterval = Duration.ofSeconds(1)

    private val watchdogId = UUID.randomUUID().toString()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val singleItemProcessor = processorFactory.create(
        watchdogId = watchdogId,
        mdcLabels = mdcLabels,
    )

    public fun start(): Job {
        return coroutineScope.launch {
            forever(logger, interval = fallbackPollInterval) {
                do {
                    val itemWasAvailable = withContext(NonCancellable) {
                        singleItemProcessor.processSingleItem(queueId, handlersByType)
                    }
                } while (itemWasAvailable && isActive)
            }
        }
    }
}

