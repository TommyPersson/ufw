package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacade
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacadeImpl
import io.tpersson.ufw.databasequeue.internal.*
import io.tpersson.ufw.databasequeue.worker.*
import jakarta.inject.Inject

public class DatabaseQueueComponent @Inject constructor(
    public val databaseQueueWorkerFactory: DatabaseQueueWorkerFactory,
    public val workQueue: WorkQueue,
    public val workQueueInternal: WorkQueueInternal,
    public val workItemsDAO: WorkItemsDAO, // TODO cleaner queue interface
    public val workItemFailuresDAO: WorkItemFailuresDAO, // TODO cleaner queue interface
    public val workQueuesDAO: WorkQueuesDAO,
    public val queueStateChecker: QueueStateChecker,
    public val adminManager: DatabaseQueueAdminFacade,
    public val config: DatabaseQueueConfig,
) {

    init {
        Migrator.registerMigrationScript(
            componentName = "database_queue",
            scriptLocation = "io/tpersson/ufw/databasequeue/migrations/postgres/liquibase.xml"
        )
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            config: DatabaseQueueConfig,
        ): DatabaseQueueComponent {
            val workItemsDAO = WorkItemsDAOImpl(
                database = databaseComponent.database,
                objectMapper = coreComponent.objectMapper,
            )

            val workItemFailuresDAO = WorkItemFailuresDAOImpl(
                database = databaseComponent.database,
            )

            val workQueuesDAO = WorkQueuesDAOImpl(
                database = databaseComponent.database,
            )

            val queueStateChecker = CachingQueueStateCheckerImpl(
                workQueuesDAO = workQueuesDAO,
                clock = coreComponent.clock,
            )

            val workQueue = WorkQueueImpl(
                workItemsDAO = workItemsDAO,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory
            )

            val processorFactory = SingleWorkItemProcessorFactoryImpl(
                workItemsDAO = workItemsDAO,
                workQueue = workQueue,
                workItemFailuresDAO = workItemFailuresDAO,
                queueStateChecker = queueStateChecker,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                meterRegistry = coreComponent.meterRegistry,
                clock = coreComponent.clock,
                config = config,
            )

            val databaseQueueWorkerFactory = DatabaseQueueWorkerFactoryImpl(
                processorFactory = processorFactory,
                workQueue = workQueue,
            )

            val databaseQueueAdminManager = DatabaseQueueAdminFacadeImpl(
                workItemsDAO = workItemsDAO,
                workQueuesDAO = workQueuesDAO,
                workItemFailuresDAO = workItemFailuresDAO,
                workQueue = workQueue,
                clock = coreComponent.clock,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
            )

            return DatabaseQueueComponent(
                databaseQueueWorkerFactory = databaseQueueWorkerFactory,
                workQueue = workQueue,
                workQueueInternal = workQueue,
                workItemsDAO = workItemsDAO,
                workItemFailuresDAO = workItemFailuresDAO,
                workQueuesDAO = workQueuesDAO,
                queueStateChecker = queueStateChecker,
                adminManager = databaseQueueAdminManager,
                config = config
            )
        }
    }
}
