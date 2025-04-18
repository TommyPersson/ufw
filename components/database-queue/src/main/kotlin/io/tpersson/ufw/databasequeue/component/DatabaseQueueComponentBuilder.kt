package io.tpersson.ufw.databasequeue.component

import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacadeImpl
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAOImpl
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.databasequeue.internal.WorkQueueImpl
import io.tpersson.ufw.databasequeue.internal.WorkQueuesDAOImpl
import io.tpersson.ufw.databasequeue.worker.CachingQueueStateCheckerImpl
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactoryImpl
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessorFactoryImpl

@UfwDslMarker
public fun UFWBuilder.Root.installDatabaseQueue(configure: DatabaseQueueComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()

    val ctx = contexts.getOrPut(DatabaseQueueComponent) { DatabaseQueueComponentBuilderContext() }
        .also(configure)

    builders.add(DatabaseQueueComponentBuilder(ctx))
}


public class DatabaseQueueComponentBuilderContext : ComponentBuilderContext<DatabaseQueueComponent>

public class DatabaseQueueComponentBuilder(
    private val context: DatabaseQueueComponentBuilderContext
) : ComponentBuilder<DatabaseQueueComponent> {

    override fun build(components: ComponentRegistryInternal): DatabaseQueueComponent {
        val workItemsDAO = WorkItemsDAOImpl(
            database = components.database.database,
            objectMapper = components.core.objectMapper,
        )

        val workItemFailuresDAO = WorkItemFailuresDAOImpl(
            database = components.database.database,
        )

        val workQueuesDAO = WorkQueuesDAOImpl(
            database = components.database.database,
        )

        val queueStateChecker = CachingQueueStateCheckerImpl(
            workQueuesDAO = workQueuesDAO,
            clock = components.core.clock,
        )

        val workQueue = WorkQueueImpl(
            workItemsDAO = workItemsDAO,
            unitOfWorkFactory = components.database.unitOfWorkFactory
        )

        val processorFactory = SingleWorkItemProcessorFactoryImpl(
            workItemsDAO = workItemsDAO,
            workQueue = workQueue,
            workItemFailuresDAO = workItemFailuresDAO,
            queueStateChecker = queueStateChecker,
            unitOfWorkFactory = components.database.unitOfWorkFactory,
            meterRegistry = components.core.meterRegistry,
            clock = components.core.clock,
            configProvider = components.core.configProvider,
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
            clock = components.core.clock,
            unitOfWorkFactory = components.database.unitOfWorkFactory,
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
        )
    }
}