package io.tpersson.ufw.durablejobs

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.durablejobs.admin.DurableJobsAdminModule
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueImpl
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueWorkersManager
import io.tpersson.ufw.durablejobs.internal.SimpleDurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAOImpl
import io.tpersson.ufw.durablejobs.internal.metrics.JobStateMetric
import io.tpersson.ufw.durablejobs.periodic.internal.*
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
                workQueue = databaseQueueComponent.workQueue,
                objectMapper = coreComponent.objectMapper,
            )

            val periodicJobSpecsProvider = PeriodicJobSpecsProviderImpl(
                jobHandlersProvider = durableJobHandlersProvider
            )

            val periodicJobsDAO = PeriodicJobsDAOImpl(
                database = databaseComponent.database
            )

            val periodicJobScheduler = PeriodicJobSchedulerImpl(
                periodicJobSpecsProvider = periodicJobSpecsProvider,
                jobQueue = jobQueue,
                queueStateChecker = databaseQueueComponent.queueStateChecker,
                databaseLocks = databaseComponent.locks,
                periodicJobsDAO = periodicJobsDAO,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                clock = coreComponent.clock,
            )

            val periodicJobManager = PeriodicJobManager(
                periodicJobSpecsProvider = periodicJobSpecsProvider,
                periodicJobScheduler = periodicJobScheduler,
                periodicJobsDAO = periodicJobsDAO,
                clock = coreComponent.clock,
            )

            val jobStateMetric = JobStateMetric(
                meterRegistry = coreComponent.meterRegistry,
                jobHandlersProvider = durableJobHandlersProvider,
                workItemsDAO = databaseQueueComponent.workItemsDAO,
                config = config,
            )

            val periodicJobsStateTracker = PeriodicJobsStateTracker(
                workQueue = databaseQueueComponent.workQueue,
                periodicJobSpecsProvider = periodicJobSpecsProvider,
                periodicJobsDAO = periodicJobsDAO,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
            )

            managedComponent.register(jobStateMetric)
            managedComponent.register(durableJobQueueWorkersManager)
            managedComponent.register(periodicJobManager)
            managedComponent.register(periodicJobsStateTracker)

            adminComponent?.register(
                DurableJobsAdminModule(
                    durableJobHandlersProvider = durableJobHandlersProvider,
                    periodicJobManager = periodicJobManager,
                    databaseQueueAdminFacade = databaseQueueComponent.adminManager,
                )
            )

            return DurableJobsComponent(
                jobQueue = jobQueue,
            )
        }
    }
}

