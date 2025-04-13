package io.tpersson.ufw.databasequeue.worker

import io.micrometer.core.instrument.MeterRegistry
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.internal.WorkQueueInternal
import jakarta.inject.Inject
import java.time.Clock

public class SingleWorkItemProcessorFactoryImpl @Inject constructor(
    private val workItemsDAO: WorkItemsDAO,
    private val workQueue: WorkQueueInternal,
    private val workItemFailuresDAO: WorkItemFailuresDAO,
    private val queueStateChecker: QueueStateChecker,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
    private val configProvider: ConfigProvider,
) : SingleWorkItemProcessorFactory {

    override fun create(
        watchdogId: String,
        adapterSettings: DatabaseQueueAdapterSettings,
    ): SingleWorkItemProcessorImpl {
        return SingleWorkItemProcessorImpl(
            watchdogId = watchdogId,
            workItemsDAO = workItemsDAO,
            workQueue = workQueue,
            workItemFailuresDAO = workItemFailuresDAO,
            queueStateChecker = queueStateChecker,
            unitOfWorkFactory = unitOfWorkFactory,
            meterRegistry = meterRegistry,
            clock = clock,
            adapterSettings = adapterSettings,
            configProvider = configProvider,
        )
    }

}