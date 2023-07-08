package io.tpersson.ufw.jobqueue

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.internal.JobQueueImpl
import io.tpersson.ufw.jobqueue.internal.JobRepositoryImpl
import java.time.Duration
import java.time.InstantSource


public interface JobQueue {
    public suspend fun <TJob : Job> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit = {}
    )

    public companion object {
        public fun create(
            config: JobQueueModuleConfig,
            databaseModuleConfig: DatabaseModuleConfig,
            instantSource: InstantSource,
            connectionProvider: ConnectionProvider,
            objectMapper: ObjectMapper,
        ): JobQueue = JobQueueImpl(
            config = config,
            instantSource = instantSource,
            jobRepository = JobRepositoryImpl(databaseModuleConfig, connectionProvider, objectMapper)
        )
    }
}

public data class JobQueueModuleConfig(
    val pollWaitTime: Duration = Duration.ofSeconds(5),
    val defaultJobTimeout: Duration = Duration.ofMinutes(10),
    val defaultJobRetention: Duration = Duration.ofDays(14),
) {
    public companion object {
        public val Default: JobQueueModuleConfig = JobQueueModuleConfig()
    }
}