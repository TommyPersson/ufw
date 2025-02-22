package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import jakarta.inject.Inject
import java.time.InstantSource

public class DatabaseQueueWorkerFactoryImpl @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
) : DatabaseQueueWorkerFactory {

    override fun create(
        queueId: String,
        handlersByType: Map<String, WorkItemHandler>,
    ): DatabaseQueueWorker {
        return DatabaseQueueWorker(
            queueId = queueId,
            handlersByType = handlersByType,
            workItemsDAO = workItemsDAO,
            unitOfWorkFactory = unitOfWorkFactory,
            clock = clock
        )
    }
}

