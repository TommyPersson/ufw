package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAOImpl
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactoryImpl
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessorFactoryImpl
import jakarta.inject.Inject

public class DatabaseQueueComponent @Inject constructor(
    public val databaseQueueWorkerFactory: DatabaseQueueWorkerFactory,
    public val workItemsDAO: WorkItemsDAO, // TODO cleaner queue interface
    public val workItemFailuresDAO: WorkItemFailuresDAO, // TODO cleaner queue interface
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

            val processorFactory = SingleWorkItemProcessorFactoryImpl(
                workItemsDAO = workItemsDAO,
                workItemFailuresDAO = workItemFailuresDAO,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                meterRegistry = coreComponent.meterRegistry,
                clock = coreComponent.clock,
                config = config,
            )

            val databaseQueueWorkerFactory = DatabaseQueueWorkerFactoryImpl(
                processorFactory = processorFactory
            )

            return DatabaseQueueComponent(
                databaseQueueWorkerFactory = databaseQueueWorkerFactory,
                workItemsDAO = workItemsDAO,
                workItemFailuresDAO = workItemFailuresDAO,
                config = config
            )
        }
    }
}
