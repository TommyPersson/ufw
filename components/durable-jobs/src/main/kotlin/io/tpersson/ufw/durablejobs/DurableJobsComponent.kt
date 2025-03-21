package io.tpersson.ufw.durablejobs

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.durablejobs.admin.DurableJobsAdminModule
import io.tpersson.ufw.durablejobs.internal.*
import io.tpersson.ufw.durablejobs.internal.metrics.JobStateMetric
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueWorkersManager
import io.tpersson.ufw.durablejobs.internal.SimpleDurableJobHandlersProvider
import io.tpersson.ufw.managed.ManagedComponent
import jakarta.inject.Inject

public class DurableJobsComponent @Inject constructor(
    public val jobQueue: DurableJobQueue,
) {
    init {
        Migrator.registerMigrationScript(
            componentName = "durable_jobs",
            scriptLocation = "io/tpersson/ufw/durablejobs/migrations/postgres/liquibase.xml"
        )
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            managedComponent: ManagedComponent,
            databaseComponent: DatabaseComponent,
            databaseQueueComponent: DatabaseQueueComponent,
            adminComponent: AdminComponent?,
            config: DurableJobsConfig,
            durableJobHandlers: Set<DurableJobHandler<*>>,
        ): DurableJobsComponent {

            val durableJobHandlersProvider = SimpleDurableJobHandlersProvider(durableJobHandlers)

            val durableJobQueueWorkersManager = DurableJobQueueWorkersManager(
                workerFactory = databaseQueueComponent.databaseQueueWorkerFactory,
                durableJobHandlersProvider = durableJobHandlersProvider,
                objectMapper = coreComponent.objectMapper,
            )

            val jobQueue = DurableJobQueueImpl(
                config = config,
                clock = coreComponent.clock,
                workItemsDAO = databaseQueueComponent.workItemsDAO,
                workItemFailuresDAO = databaseQueueComponent.workItemFailuresDAO,
                workQueuesDAO = databaseQueueComponent.workQueuesDAO,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                objectMapper = coreComponent.objectMapper,
            )

            val jobStateMetric = JobStateMetric(
                meterRegistry = coreComponent.meterRegistry,
                jobHandlersProvider = durableJobHandlersProvider,
                workItemsDAO = databaseQueueComponent.workItemsDAO,
                config = config,
            )

            managedComponent.register(jobStateMetric)
            managedComponent.register(durableJobQueueWorkersManager)

            adminComponent?.register(DurableJobsAdminModule(
                durableJobHandlersProvider = durableJobHandlersProvider,
                jobQueue = jobQueue,
                clock = coreComponent.clock
            ))

            return DurableJobsComponent(
                jobQueue = jobQueue,
            )
        }
    }
}

