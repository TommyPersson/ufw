package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessor.ProcessingResult
import kotlinx.coroutines.*
import java.time.Duration
import java.util.*

public class DatabaseQueueWorker(
    private val queueId: WorkItemQueueId,
    private val handlersByType: Map<String, WorkItemHandler<*>>,
    processorFactory: SingleWorkItemProcessorFactory,
    adapterSettings: DatabaseQueueAdapterSettings,
) {
    private val logger = createLogger()

    private val fallbackPollInterval = Duration.ofSeconds(1)

    private val watchdogId = UUID.randomUUID().toString()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val singleItemProcessor = processorFactory.create(
        watchdogId = watchdogId,
        adapterSettings = adapterSettings,
    )

    public fun start(): Job {
        return coroutineScope.launch {
            forever(logger, interval = fallbackPollInterval) {
                do {
                    val result = withContext(NonCancellable) {
                        singleItemProcessor.processSingleItem(queueId, handlersByType)
                    }
                } while (result == ProcessingResult.PROCESSED && isActive)
            }
        }
    }
}

