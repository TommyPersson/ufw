package io.tpersson.ufw.databasequeue.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.WorkQueue
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacade
import io.tpersson.ufw.databasequeue.internal.*
import io.tpersson.ufw.databasequeue.worker.*
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class DatabaseQueueComponent @Inject constructor(
    public val databaseQueueWorkerFactory: DatabaseQueueWorkerFactory,
    public val workQueue: WorkQueue,
    public val workQueueInternal: WorkQueueInternal,
    public val workItemsDAO: WorkItemsDAO, // TODO cleaner queue interface
    public val workItemFailuresDAO: WorkItemFailuresDAO, // TODO cleaner queue interface
    public val workQueuesDAO: WorkQueuesDAO,
    public val queueStateChecker: QueueStateChecker,
    public val adminManager: DatabaseQueueAdminFacade,
) : Component<DatabaseQueueComponent> {

    init {
        Migrator.registerMigrationScript(
            componentName = "database_queue",
            scriptLocation = "io/tpersson/ufw/databasequeue/migrations/postgres/liquibase.xml"
        )
    }

    public companion object : ComponentKey<DatabaseQueueComponent> {
    }
}

public val ComponentRegistry.databaseQueue: DatabaseQueueComponent get() = get(DatabaseQueueComponent)