package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.jobqueue.internal.*
import io.tpersson.ufw.jobqueue.internal.metrics.JobStateMetric
import io.tpersson.ufw.managed.ManagedComponent
import jakarta.inject.Inject

public class JobQueueComponent @Inject constructor(
    public val jobQueue: JobQueue,
    internal val jobsDAO: JobsDAO,
    internal val jobFailureRepository: JobFailureRepository,
    internal val staleJobRescheduler: StaleJobRescheduler,
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
            databaseComponent: DatabaseComponent,
            config: JobQueueConfig,
            jobHandlers: Set<JobHandler<*>>,
        ): JobQueueComponent {

            val jobRepository = JobsDAOImpl(
                database = databaseComponent.database,
                objectMapper = coreComponent.objectMapper
            )

            val jobFailureRepository = JobFailureRepositoryImpl(
                database = databaseComponent.database,
            )

            val jobQueue = JobQueueImpl(
                config = config,
                clock = coreComponent.clock,
                jobsDAO = jobRepository,
                jobFailureRepository = jobFailureRepository
            )

            val jobHandlersProvider = SimpleJobHandlersProvider(jobHandlers)

            val jobQueueRunner = JobQueueRunner(
                jobQueue = jobQueue,
                jobsDAO = jobRepository,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                jobHandlersProvider = jobHandlersProvider,
                clock = coreComponent.clock,
                config = config
            )

            val staleJobRescheduler = StaleJobRescheduler(
                jobsDAO = jobRepository,
                clock = coreComponent.clock,
                config = config,
            )

            val expiredJobReaper = ExpiredJobReaper(
                jobsDAO = jobRepository,
                clock = coreComponent.clock,
                config = config
            )

            val jobStateMetric = JobStateMetric(
                meter = coreComponent.meter,
                jobHandlersProvider = jobHandlersProvider,
                jobsDAO = jobRepository,
                config = config,
            )

            managedComponent.register(jobQueueRunner)
            managedComponent.register(staleJobRescheduler)
            managedComponent.register(expiredJobReaper)
            managedComponent.register(jobStateMetric)

            return JobQueueComponent(
                jobQueue = jobQueue,
                jobsDAO = jobRepository,
                jobFailureRepository = jobFailureRepository,
                staleJobRescheduler = staleJobRescheduler
            )
        }
    }
}

