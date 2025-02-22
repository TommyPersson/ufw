package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactoryImpl
import jakarta.inject.Inject

public class DatabaseQueueComponent @Inject constructor(
    public val databaseQueueWorkerFactory: DatabaseQueueWorkerFactory
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
        ): DatabaseQueueComponent {
            val databaseQueueWorkerFactory = DatabaseQueueWorkerFactoryImpl(
                workItemsDAO = WorkItemsDAOImpl(
                    database = databaseComponent.database,
                    objectMapper = coreComponent.objectMapper,
                ),
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                clock = coreComponent.clock,
            )

            return DatabaseQueueComponent(databaseQueueWorkerFactory)
        }
    }
}
