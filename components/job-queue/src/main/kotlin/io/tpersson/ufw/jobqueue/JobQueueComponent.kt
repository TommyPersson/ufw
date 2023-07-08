package io.tpersson.ufw.jobqueue

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.jobqueue.internal.JobQueueImpl
import io.tpersson.ufw.jobqueue.internal.JobRepositoryImpl
import io.tpersson.ufw.managed.Managed

public class JobQueueComponent private constructor(
    public val jobQueue: JobQueue,
    public val jobQueueRunner: Managed,
) {
    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            jobHandlers: Set<JobHandler<*>>,
        ): JobQueueComponent {
            Migrator.registerMigrationScript("io/tpersson/ufw/jobqueue/migrations/postgres/liquibase.xml")

            val objectMapper = jacksonObjectMapper().findAndRegisterModules()

            val config = JobQueueModuleConfig()

            val jobRepository = JobRepositoryImpl(
                databaseModuleConfig = databaseComponent.config,
                connectionProvider = databaseComponent.connectionProvider,
                objectMapper = objectMapper
            )

            val jobQueue = JobQueueImpl(
                config = config,
                instantSource = coreComponent.instantSource,
                jobRepository = jobRepository
            )

            val jobHandlersProvider = SimpleJobHandlersProvider(jobHandlers)

            val jobQueueRunner = JobQueueRunner(jobHandlersProvider)

            return JobQueueComponent(jobQueue, jobQueueRunner)
        }
    }
}

