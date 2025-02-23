package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import jakarta.inject.Inject
import kotlinx.coroutines.*
import java.time.Duration
import java.time.InstantSource
import java.util.*

public class DatabaseQueueWorker @Inject constructor(
    private val queueId: String,
    private val handlersByType: Map<String, WorkItemHandler<*>>,
    workItemsDAO: WorkItemsDAO,
    workItemFailuresDAO: WorkItemFailuresDAO,
    unitOfWorkFactory: UnitOfWorkFactory,
    clock: InstantSource,
) {
    private val logger = createLogger<DatabaseQueueWorker>()

    private val fallbackPollInterval = Duration.ofSeconds(1)

    private val watchdogId = UUID.randomUUID().toString()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val singleItemProcessor = SingleWorkItemProcessor(
        watchdogId = watchdogId,
        workItemsDAO = workItemsDAO,
        workItemFailuresDAO = workItemFailuresDAO,
        unitOfWorkFactory = unitOfWorkFactory,
        clock = clock
    )

    public fun start(): Job {
        return coroutineScope.launch {
            forever(logger, interval = fallbackPollInterval) {
                do {
                    val itemWasAvailable = singleItemProcessor.processSingleItem(queueId, handlersByType)
                } while (itemWasAvailable)
            }
        }
    }
}