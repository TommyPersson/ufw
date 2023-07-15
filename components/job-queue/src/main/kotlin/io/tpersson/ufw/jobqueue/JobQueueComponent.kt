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
    internal val jobRepository: JobRepository,
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

            val jobRepository = JobRepositoryImpl(
                database = databaseComponent.database,
                objectMapper = coreComponent.objectMapper
            )

            val jobFailureRepository = JobFailureRepositoryImpl(
                database = databaseComponent.database,
            )

            val jobQueue = JobQueueImpl(
                config = config,
                clock = coreComponent.clock,
                jobRepository = jobRepository,
                jobFailureRepository = jobFailureRepository
            )

            val jobHandlersProvider = SimpleJobHandlersProvider(jobHandlers)

            val jobQueueRunner = JobQueueRunner(
                jobQueue = jobQueue,
                jobRepository = jobRepository,
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                jobHandlersProvider = jobHandlersProvider,
                clock = coreComponent.clock,
                config = config
            )

            val staleJobRescheduler = StaleJobRescheduler(
                jobRepository = jobRepository,
                clock = coreComponent.clock,
                config = config,
            )

            val expiredJobReaper = ExpiredJobReaper(
                jobRepository = jobRepository,
                clock = coreComponent.clock,
                config = config
            )

            val jobStateMetric = JobStateMetric(
                meter = coreComponent.meter,
                jobHandlersProvider = jobHandlersProvider,
                jobRepository = jobRepository,
                config = config,
            )

            managedComponent.register(jobQueueRunner)
            managedComponent.register(staleJobRescheduler)
            managedComponent.register(expiredJobReaper)
            managedComponent.register(jobStateMetric)

            return JobQueueComponent(
                jobQueue = jobQueue,
                jobRepository = jobRepository,
                jobFailureRepository = jobFailureRepository,
                staleJobRescheduler = staleJobRescheduler
            )
        }
    }
}

