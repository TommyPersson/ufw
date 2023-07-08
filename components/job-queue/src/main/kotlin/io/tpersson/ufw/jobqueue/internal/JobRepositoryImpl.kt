package io.tpersson.ufw.jobqueue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobQueueId
import jakarta.inject.Inject
import java.time.Duration
import java.time.Instant

public class JobRepositoryImpl @Inject constructor(
    private val databaseModuleConfig: DatabaseModuleConfig,
    private val connectionProvider: ConnectionProvider,
    private val objectMapper: ObjectMapper,
) : JobRepository {

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
