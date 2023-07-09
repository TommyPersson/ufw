package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.jobqueue.internal.*
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject

public class JobQueueComponent @Inject constructor(
    public val jobQueue: JobQueue,
    public val managedInstances: Set<Managed>,
    internal val jobRepository: JobRepository,
    internal val jobFailureRepository: JobFailureRepository
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
            databaseComponent: DatabaseComponent,
            jobHandlers: Set<JobHandler<*>>,
        ): JobQueueComponent {
            val config = JobQueueModuleConfig()

            val jobRepository = JobRepositoryImpl(
                databaseModuleConfig = databaseComponent.config,
                connectionProvider = databaseComponent.connectionProvider,
                ufwObjectMapper = coreComponent.objectMapper
            )

            val jobFailureRepository = JobFailureRepositoryImpl(
                connectionProvider = databaseComponent.connectionProvider
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
                clock = coreComponent.clock
            )

            return JobQueueComponent(
                jobQueue = jobQueue,
                managedInstances = setOf(jobQueueRunner),
                jobRepository = jobRepository,
                jobFailureRepository = jobFailureRepository
            )
        }
    }
}

