package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.databasequeue.DatabaseQueueMdcLabels
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import jakarta.inject.Inject
import java.time.InstantSource

public class DatabaseQueueWorkerFactoryImpl @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val workItemFailuresDAO: WorkItemFailuresDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
    private val config: DatabaseQueueConfig,
) : DatabaseQueueWorkerFactory {

    override fun create(
        queueId: String,
        handlersByType: Map<String, WorkItemHandler<*>>,
        mdcLabels: DatabaseQueueMdcLabels
    ): DatabaseQueueWorker {
        return DatabaseQueueWorker(
            queueId = queueId,
            handlersByType = handlersByType,
            workItemsDAO = workItemsDAO,
            workItemFailuresDAO = workItemFailuresDAO,
            unitOfWorkFactory = unitOfWorkFactory,
            clock = clock,
            mdcLabels = mdcLabels,
            config = config,
        )
    }
}

