package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.jobqueue.admin.JobQueueAdminModule
import io.tpersson.ufw.jobqueue.internal.*
import io.tpersson.ufw.jobqueue.internal.metrics.JobStateMetric
import io.tpersson.ufw.jobqueue.internal.DurableJobQueueWorkersManager
import io.tpersson.ufw.jobqueue.internal.SimpleDurableJobHandlersProvider
import io.tpersson.ufw.managed.ManagedComponent
import jakarta.inject.Inject

public class JobQueueComponent @Inject constructor(
    public val jobQueue: JobQueue,
) {
    init {
        Migrator.registerMigrationScript(
            componentName = "job_queue",
            scriptLocation = "io/tpersson/ufw/jobqueue/migrations/postgres/liquibase.xml"
        )
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            managedComponent: ManagedComponent,
            databaseQueueComponent: DatabaseQueueComponent,
            adminComponent: AdminComponent?,
            config: JobQueueConfig,
            durableJobHandlers: Set<DurableJobHandler<*>>,
        ): JobQueueComponent {

            val durableJobHandlersProvider = SimpleDurableJobHandlersProvider(durableJobHandlers)

            val durableJobQueueWorkersManager = DurableJobQueueWorkersManager(
                workerFactory = databaseQueueComponent.databaseQueueWorkerFactory,
                durableJobHandlersProvider = durableJobHandlersProvider,
                objectMapper = coreComponent.objectMapper,
            )



            val jobQueue = JobQueueImpl(
                config = config,
                clock = coreComponent.clock,
                workItemsDAO = databaseQueueComponent.workItemsDAO,
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

            adminComponent?.register(JobQueueAdminModule(durableJobHandlersProvider, jobQueue))

            return JobQueueComponent(
                jobQueue = jobQueue,
            )
        }
    }
}

