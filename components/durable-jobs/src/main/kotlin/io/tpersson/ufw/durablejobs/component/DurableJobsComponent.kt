package io.tpersson.ufw.durablejobs.component

import io.tpersson.ufw.admin.component.AdminComponent
import io.tpersson.ufw.core.component.CoreComponent
import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.database.component.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.databasequeue.component.DatabaseQueueComponent
import io.tpersson.ufw.durablejobs.DurableJobHandler
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.admin.DurableJobsAdminModule
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueImpl
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueWorkersManager
import io.tpersson.ufw.durablejobs.internal.SimpleDurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAOImpl
import io.tpersson.ufw.durablejobs.internal.metrics.JobStateMetric
import io.tpersson.ufw.durablejobs.periodic.internal.*
import io.tpersson.ufw.managed.component.ManagedComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton

public interface DurableJobsComponent : Component<DurableJobsComponent> {
    public val jobQueue: DurableJobQueue

    public fun register(handler: DurableJobHandler<*>)
}

public interface DurableJobsComponentInternal : DurableJobsComponent {
    public val jobHandlers: DurableJobHandlersProvider
}

@Singleton
public class DurableJobsComponentImpl @Inject constructor(
    public override val jobQueue: DurableJobQueue,
    public override val jobHandlers: DurableJobHandlersProvider,
) : DurableJobsComponentInternal {
    init {
        Migrator.registerMigrationScript(
            componentName = "durable_jobs",
            scriptLocation = "io/tpersson/ufw/durablejobs/migrations/postgres/liquibase.xml"
        )
    }

    override fun register(handler: DurableJobHandler<*>) {
        jobHandlers.add(handler)
    }

    public companion object : ComponentKey<DurableJobsComponent> {
        public fun create(
            coreComponent: CoreComponent,
            managedComponent: ManagedComponent,
            databaseComponent: DatabaseComponent,
            databaseQueueComponent: DatabaseQueueComponent,
            adminComponent: AdminComponent,
            durableJobHandlers: Set<DurableJobHandler<*>>,
        ): DurableJobsComponentImpl {

            val durableJobHandlersProvider = SimpleDurableJobHandlersProvider(durableJobHandlers.toMutableSet())

            val durableJobQueueWorkersManager = DurableJobQueueWorkersManager(
                workerFactory = databaseQueueComponent.databaseQueueWorkerFactory,
                durableJobHandlersProvider = durableJobHandlersProvider,
                objectMapper = coreComponent.objectMapper,
            )

            val jobQueue = DurableJobQueueImpl(
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
                appInfoProvider = coreComponent.appInfoProvider,
                clock = coreComponent.clock,
            )

            val periodicJobManager = PeriodicJobManager(
                periodicJobSpecsProvider = periodicJobSpecsProvider,
                periodicJobScheduler = periodicJobScheduler,
                periodicJobsDAO = periodicJobsDAO,
                configProvider = coreComponent.configProvider,
                clock = coreComponent.clock,
            )

            val jobStateMetric = JobStateMetric(
                meterRegistry = coreComponent.meterRegistry,
                jobHandlersProvider = durableJobHandlersProvider,
                workItemsDAO = databaseQueueComponent.workItemsDAO,
                configProvider = coreComponent.configProvider,
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

            adminComponent.register(
                DurableJobsAdminModule(
                    durableJobHandlersProvider = durableJobHandlersProvider,
                    periodicJobManager = periodicJobManager,
                    databaseQueueAdminFacade = databaseQueueComponent.adminManager,
                )
            )

            return DurableJobsComponentImpl(
                jobQueue = jobQueue,
                jobHandlers = durableJobHandlersProvider,
            )
        }
    }
}

