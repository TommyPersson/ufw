package io.tpersson.ufw.jobqueue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProvider
import io.tpersson.ufw.db.typedqueries.TypedUpdate
import io.tpersson.ufw.db.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobId
import io.tpersson.ufw.jobqueue.JobQueueId
import jakarta.inject.Inject
import org.flywaydb.core.Flyway
import java.time.Duration
import java.time.Instant

public class JobRepositoryImpl @Inject constructor(
    private val dbModuleConfig: DbModuleConfig,
    private val connectionProvider: ConnectionProvider,
    private val objectMapper: ObjectMapper,
) : JobRepository {

    init {
        JobQueueDbMigrator.migrate(connectionProvider)
    }

    override suspend fun insert(job: InternalJob<*>, unitOfWork: UnitOfWork) {
        val jobData = JobData(
            jobId = job.job.jobId.value,
            jobJson = objectMapper.writeValueAsString(job.job),
            scheduledFor = job.scheduledFor,
            timeout = job.timeout,
            retentionOnFailure = job.retentionOnFailure,
            retentionOnSuccess = job.retentionOnSuccess
        )

        unitOfWork.add(Queries.InsertJob(jobData))
    }

    override fun <TJob : Job> getNext(jobQueueId: JobQueueId<TJob>): InternalJob<TJob>? {
        TODO("Not yet implemented")
    }

    private object Queries {
        private val TableName = "ufw__job_queue__jobs"

        data class InsertJob(
            val data: JobData
        ) : TypedUpdate(
            """
            INSERT INTO $TableName (
                id
            ) VALUES (
                :data.jobId
            )
            """
        )
    }

    internal class JobData(
        val jobId: String,
        val jobJson: String,
        val scheduledFor: Instant,
        val timeout: Duration,
        val retentionOnFailure: Duration,
        val retentionOnSuccess: Duration,
    )
}

internal object JobQueueDbMigrator {

    private var hasMigrated = false

    fun migrate(connectionProvider: ConnectionProvider) {
        synchronized(this) {
            if (hasMigrated) {
                return
            }

            Flyway.configure()
                .dataSource(connectionProvider.dataSource)
                .loggers("slf4j")
                .baselineOnMigrate(true)
                .locations("classpath:io/tpersson/ufw/jobqueue/migrations/postgres")
                .table("ufw__job_queue__flyway")
                .load().also {
                    it.migrate()
                }

            hasMigrated = true
        }
    }
}