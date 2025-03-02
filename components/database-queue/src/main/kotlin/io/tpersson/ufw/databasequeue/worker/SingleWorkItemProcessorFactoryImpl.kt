package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import jakarta.inject.Inject
import java.time.InstantSource

public class SingleWorkItemProcessorFactoryImpl @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val workItemFailuresDAO: WorkItemFailuresDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val clock: InstantSource,
    private val config: DatabaseQueueConfig,
) : SingleWorkItemProcessorFactory {

    override fun create(
        watchdogId: String,
        adapterSettings: DatabaseQueueAdapterSettings,
    ): SingleWorkItemProcessorImpl {
        return SingleWorkItemProcessorImpl(
            watchdogId = watchdogId,
            workItemsDAO = workItemsDAO,
            workItemFailuresDAO = workItemFailuresDAO,
            unitOfWorkFactory = unitOfWorkFactory,
            clock = clock,
            adapterSettings = adapterSettings,
            config = config,
        )
    }

}