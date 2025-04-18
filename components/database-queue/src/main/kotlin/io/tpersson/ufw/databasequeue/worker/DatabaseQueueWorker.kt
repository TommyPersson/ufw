package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.databasequeue.*
import io.tpersson.ufw.databasequeue.configuration.DatabaseQueue
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessor.ProcessingResult
import kotlinx.coroutines.*
import java.util.*

public class DatabaseQueueWorker(
    private val queueId: WorkItemQueueId,
    private val handlersByType: Map<String, WorkItemHandler<*>>,
    private val workQueue: WorkQueue,
    processorFactory: SingleWorkItemProcessorFactory,
    adapterSettings: DatabaseQueueAdapterSettings,
    private val configProvider: ConfigProvider,
) {
    private val logger = createLogger()

    private val fallbackPollInterval = configProvider.get(Configs.DatabaseQueue.FallbackPollInterval)

    private val watchdogId = UUID.randomUUID().toString()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val singleItemProcessor = processorFactory.create(
        watchdogId = watchdogId,
        adapterSettings = adapterSettings,
    )

    public fun start(): Job {
        return coroutineScope.launch {
            val signal = createItemAvailableSignal()

            forever(logger) {
                signal.wait(fallbackPollInterval)

                do {
                    val result = withContext(NonCancellable) {
                        singleItemProcessor.processSingleItem(queueId, handlersByType)
                    }
                } while (result == ProcessingResult.PROCESSED && isActive)
            }
        }
    }

    private fun CoroutineScope.createItemAvailableSignal(): ConsumerSignal {
        val signal = ConsumerSignal()

        launch {
            workQueue.stateChanges.collect { change ->
                if (change.queueId == queueId && change.toState == WorkItemState.SCHEDULED) {
                    signal.signal()
                }
            }
        }

        return signal
    }
}

