package io.tpersson.ufw.jobqueue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.useInTransaction
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.typedqueries.selectSingle
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobId
import io.tpersson.ufw.jobqueue.JobQueueId
import io.tpersson.ufw.jobqueue.JobState
import jakarta.inject.Inject
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import kotlin.math.exp

public class JobRepositoryImpl @Inject constructor(
    private val databaseModuleConfig: DatabaseModuleConfig,
    private val connectionProvider: ConnectionProvider,
    private val objectMapper: ObjectMapper,
) : JobRepository {

    // TODO implement stale job detection

    override suspend fun insert(job: InternalJob<*>, unitOfWork: UnitOfWork) {
        val jobData = JobData(
            uid = 0,
            id = job.job.jobId.value,
            type = job.job::class.simpleName!!,
            state = job.state.id,
            json = objectMapper.writeValueAsString(job.job),
            createdAt = job.createdAt,
            scheduledFor = job.scheduledFor,
            stateChangedAt = job.stateChangedAt,
            expireAt = null,
        )

        unitOfWork.add(Queries.InsertJob(jobData))
    }

    override fun <TJob : Job> getNext(jobQueueId: JobQueueId<TJob>): InternalJob<TJob>? {
        val jobData = connectionProvider.get().useInTransaction {
            it.selectSingle(Queries.SelectNextJob(jobQueueId.typeName))
        } ?: return null

        return toInternalJob(jobData, jobQueueId)
    }

    override fun <TJob : Job> getById(jobQueueId: JobQueueId<TJob>, jobId: JobId): InternalJob<TJob>? {
        val jobData = connectionProvider.get().useInTransaction {
            it.selectSingle(Queries.SelectById(jobId.value))
        } ?: return null

        return toInternalJob(jobData, jobQueueId)
    }

    override fun <TJob : Job> markAsInProgress(job: InternalJob<TJob>, now: Instant, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.MarkJobAsInProgress(id = job.job.jobId.value, timestamp = now))
    }

    override fun <TJob : Job> maskAsSuccessful(job: InternalJob<TJob>, now: Instant, unitOfWork: UnitOfWork) {
        val expireAt = now + Duration.ofDays(7) // TODO get from job or other parameter
        unitOfWork.add(Queries.MarkJobAsSuccessful(id = job.job.jobId.value, timestamp = now, expireAt = expireAt))
    }

    private fun <TJob : Job> toInternalJob(
        jobData: JobData,
        jobQueueId: JobQueueId<TJob>
    ): InternalJob<TJob> {
        val job = objectMapper.readValue(jobData.json, jobQueueId.jobType.java)

        return InternalJob(
            uid = jobData.uid,
            job = job,
            state = JobState.fromId(jobData.state),
            createdAt = jobData.createdAt,
            scheduledFor = jobData.scheduledFor,
            stateChangedAt = jobData.stateChangedAt,
            expireAt = jobData.expireAt,
        )
    }

    private object Queries {
        private val TableName = "ufw__job_queue__jobs"

        data class InsertJob(
            val data: JobData
        ) : TypedUpdate(
            """
            INSERT INTO $TableName (          
                id,          
                type,         
                state,        
                json,         
                created_at,   
                scheduled_for,
                state_changed_at, 
                expire_at        
            ) VALUES (
                :data.id,
                :data.type,
                :data.state,
                :data.json, 
                :data.createdAt,
                :data.scheduledFor,
                :data.stateChangedAt,
                :data.expireAt
            )
            """
        )

        data class MarkJobAsInProgress(
            val id: String,
            val timestamp: Instant,
            val toState: Int = JobState.InProgress.id,
        ) : TypedUpdate(
            """
            UPDATE $TableName
            SET state = :toState,
                state_changed_at = :timestamp
            WHERE state = ${JobState.Scheduled.id}
              AND id = :id
            """.trimIndent()
        )

        data class MarkJobAsSuccessful(
            val id: String,
            val timestamp: Instant,
            val expireAt: Instant,
            val toState: Int = JobState.Successful.id,
        ) : TypedUpdate(
            """
            UPDATE $TableName
            SET state = :toState,
                state_changed_at = :timestamp,
                expire_at = :expireAt
            WHERE state = ${JobState.InProgress.id}
              AND id = :id
            """.trimIndent()
        )

        data class SelectNextJob(
            val type: String,
        ) : TypedSelect<JobData>(
            """
            SELECT * 
            FROM $TableName
            WHERE state = ${JobState.Scheduled.id}
              AND type = :type
            ORDER BY scheduled_for ASC
            LIMIT 1
            """.trimIndent()
        )

        data class SelectById(
            val id: String,
        ) : TypedSelect<JobData>(
            "SELECT * FROM $TableName WHERE id = :id"
        )
    }

    internal class JobData(
        val uid: Long?,
        val id: String,
        val type: String,
        val state: Int,
        val json: String,
        val createdAt: Instant,
        val scheduledFor: Instant,
        val stateChangedAt: Instant,
        val expireAt: Instant?,
    )
}