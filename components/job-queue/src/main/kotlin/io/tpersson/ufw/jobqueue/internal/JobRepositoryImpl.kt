package io.tpersson.ufw.jobqueue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.useInTransaction
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.typedqueries.selectList
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
import kotlin.math.min

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

        unitOfWork.add(Queries.Updates.InsertJob(jobData))
    }

    override suspend fun <TJob : Job> getNext(jobQueueId: JobQueueId<TJob>, now: Instant): InternalJob<TJob>? {
        val jobData = connectionProvider.get().useInTransaction {
            it.selectSingle(Queries.Selects.SelectNextJob(jobQueueId.typeName, now))
        } ?: return null

        return toInternalJob(jobData, jobQueueId)
    }

    override suspend fun <TJob : Job> getById(jobQueueId: JobQueueId<TJob>, jobId: JobId): InternalJob<TJob>? {
        val jobData = connectionProvider.get().useInTransaction {
            it.selectSingle(Queries.Selects.SelectById(jobId.value))
        } ?: return null

        return toInternalJob(jobData, jobQueueId)
    }

    override suspend fun <TJob : Job> markAsInProgress(job: InternalJob<TJob>, now: Instant, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.MarkJobAsInProgress(id = job.job.jobId.value, timestamp = now))
    }

    override suspend fun <TJob : Job> markAsSuccessful(job: InternalJob<TJob>, now: Instant, unitOfWork: UnitOfWork) {
        val expireAt = now + Duration.ofDays(7) // TODO get from job or other parameter
        unitOfWork.add(
            Queries.Updates.MarkJobAsSuccessful(
                id = job.job.jobId.value,
                timestamp = now,
                expireAt = expireAt
            )
        )
    }

    override suspend fun <TJob : Job> markAsFailed(job: InternalJob<TJob>, now: Instant, unitOfWork: UnitOfWork) {
        val expireAt = now + Duration.ofDays(7) // TODO get from job or other parameter
        unitOfWork.add(
            Queries.Updates.MarkJobAsFailed(
                id = job.job.jobId.value,
                timestamp = now,
                expireAt = expireAt
            )
        )
    }

    override suspend fun <TJob : Job> markAsScheduled(
        job: InternalJob<TJob>,
        now: Instant,
        scheduleFor: Instant,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkJobAsScheduled(
                id = job.job.jobId.value,
                timestamp = now,
                scheduledFor = scheduleFor
            )
        )
    }

    override suspend fun debugGetAllJobs(): List<InternalJob<*>> {
        val jobData = connectionProvider.get().useInTransaction {
            it.selectList(Queries.Selects.DebugGetAll)
        }

        return jobData.map { toInternalJob(it, JobQueueId(StubJob::class)) }
    }

    override suspend fun debugTruncate(unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.DebugTruncate)
    }

    public data class StubJob(override val jobId: JobId) : Job

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

        object Selects {
            data class SelectNextJob(
                val type: String,
                val now: Instant,
            ) : TypedSelect<JobData>(
                """
                SELECT * 
                FROM $TableName
                WHERE state = ${JobState.Scheduled.id}
                  AND type = :type
                  AND scheduled_for <= :now
                ORDER BY scheduled_for ASC
                LIMIT 1
                """.trimIndent()
            )

            data class SelectById(
                val id: String,
            ) : TypedSelect<JobData>(
                "SELECT * FROM $TableName WHERE id = :id"
            )

            object DebugGetAll : TypedSelect<JobData>("SELECT * FROM $TableName")
        }

        object Updates {
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
                ON CONFLICT (id) DO NOTHING        
                """,
                minimumAffectedRows = 0
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

            data class MarkJobAsFailed(
                val id: String,
                val timestamp: Instant,
                val expireAt: Instant,
                val toState: Int = JobState.Failed.id,
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

            data class MarkJobAsScheduled(
                val id: String,
                val timestamp: Instant,
                val scheduledFor: Instant,
                val toState: Int = JobState.Scheduled.id,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    scheduled_for = :scheduledFor
                WHERE state = ${JobState.InProgress.id}
                  AND id = :id
                """.trimIndent()
            )

            object DebugTruncate : TypedUpdate("DELETE FROM $TableName", minimumAffectedRows = 0)
        }
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