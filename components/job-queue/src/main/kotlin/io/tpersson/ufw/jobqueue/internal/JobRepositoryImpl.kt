package io.tpersson.ufw.jobqueue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.exceptions.TypedUpdateMinimumAffectedRowsException
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.Job
import io.tpersson.ufw.jobqueue.JobId
import io.tpersson.ufw.jobqueue.JobQueueId
import io.tpersson.ufw.jobqueue.JobState
import io.tpersson.ufw.jobqueue.internal.exceptions.JobOwnershipLostException
import io.tpersson.ufw.jobqueue.internal.metrics.JobQueueStatistics
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Instant
import java.util.*

@Singleton
public class JobRepositoryImpl @Inject constructor(
    private val database: Database,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : JobRepository {

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
            watchdogTimestamp = null,
            watchdogOwner = null
        )

        unitOfWork.add(Queries.Updates.InsertJob(jobData))
    }

    override suspend fun <TJob : Job> getNext(jobQueueId: JobQueueId<TJob>, now: Instant): InternalJob<TJob>? {
        val jobData = database.select(Queries.Selects.SelectNextJob(jobQueueId.typeName, now))
            ?: return null

        return toInternalJob(jobData, jobQueueId)
    }

    override suspend fun <TJob : Job> getById(jobQueueId: JobQueueId<TJob>, jobId: JobId): InternalJob<TJob>? {
        val jobData = database.select(Queries.Selects.SelectById(jobId.value))
            ?: return null

        return toInternalJob(jobData, jobQueueId)
    }

    override suspend fun <TJob : Job> markAsInProgress(
        job: InternalJob<TJob>,
        now: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork,
    ) {
        unitOfWork.add(
            Queries.Updates.MarkJobAsInProgress(
                id = job.job.jobId.value,
                timestamp = now,
                watchdogOwner = watchdogId
            )
        )
    }

    override suspend fun <TJob : Job> markAsSuccessful(
        job: InternalJob<TJob>,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkJobAsSuccessful(
                id = job.job.jobId.value,
                timestamp = now,
                expireAt = expireAt,
                expectedWatchdogOwner = watchdogId
            ),
            ::exceptionMapper
        )
    }

    override suspend fun <TJob : Job> markAsFailed(
        job: InternalJob<TJob>,
        now: Instant,
        expireAt: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkJobAsFailed(
                id = job.job.jobId.value,
                timestamp = now,
                expireAt = expireAt,
                expectedWatchdogOwner = watchdogId
            ),
            ::exceptionMapper
        )
    }

    override suspend fun <TJob : Job> markAsScheduled(
        job: InternalJob<TJob>,
        now: Instant,
        scheduleFor: Instant,
        watchdogId: String,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(
            Queries.Updates.MarkJobAsScheduled(
                id = job.job.jobId.value,
                timestamp = now,
                scheduledFor = scheduleFor,
                expectedWatchdogOwner = watchdogId
            ),
            ::exceptionMapper
        )
    }

    override suspend fun markStaleJobsAsScheduled(
        now: Instant,
        staleIfWatchdogOlderThan: Instant
    ): Int {
        return database.update(
            Queries.Updates.MarkStaleJobsAsScheduled(
                timestamp = now,
                staleIfWatchdogOlderThan = staleIfWatchdogOlderThan
            )
        )
    }

    override suspend fun <TJob : Job> updateWatchdog(
        job: InternalJob<TJob>,
        now: Instant,
        watchdogId: String
    ): Boolean {
        val affectedRows = database.update(
            Queries.Updates.UpdateWatchdog(
                jobUid = job.uid!!,
                watchdogTimestamp = now,
                expectedWatchdogOwner = watchdogId
            ),
            ::exceptionMapper
        )

        return affectedRows > 0
    }

    override suspend fun deleteExpiredJobs(now: Instant): Int {
        return database.update(Queries.Updates.DeleteExpiredJobs(now))
    }

    override suspend fun <TJob : Job> getQueueStatistics(queueId: JobQueueId<TJob>): JobQueueStatistics<TJob> {
        val data = database.selectList(Queries.Selects.GetStatistics(queueId.typeName))

        val map = data.associateBy { JobState.fromId(it.stateId) }

        return JobQueueStatistics(
            queueId = queueId,
            numScheduled = map[JobState.Scheduled]?.count ?: 0,
            numInProgress = map[JobState.InProgress]?.count ?: 0,
            numSuccessful = map[JobState.Successful]?.count ?: 0,
            numFailed = map[JobState.Failed]?.count ?: 0,
        )
    }

    override suspend fun debugGetAllJobs(): List<InternalJob<*>> {
        val jobData = database.selectList(Queries.Selects.DebugGetAll)

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

    private fun exceptionMapper(exception: Exception): Exception {
        throw when (exception) {
            is TypedUpdateMinimumAffectedRowsException -> {
                if (exception.query::class in Queries.Updates.queriesThatRequireOwnership) {
                    JobOwnershipLostException(exception)
                } else exception
            }

            else -> exception
        }
    }

    internal object Queries {
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

            data class GetStatistics(
                val queueId: String
            ) : TypedSelect<StatisticsData>(
                """
                SELECT count(*) as count, state as state_id
                FROM ufw__job_queue__jobs
                WHERE type = :queueId
                GROUP BY state
                """.trimIndent()
            )

            object DebugGetAll : TypedSelect<JobData>("SELECT * FROM $TableName")
        }

        object Updates {
            val queriesThatRequireOwnership = listOf(
                MarkJobAsSuccessful::class,
                MarkJobAsFailed::class,
                MarkJobAsScheduled::class,
                UpdateWatchdog::class
            )

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
                    expire_at,
                    watchdog_timestamp
                ) VALUES (
                    :data.id,
                    :data.type,
                    :data.state,
                    :data.json, 
                    :data.createdAt,
                    :data.scheduledFor,
                    :data.stateChangedAt,
                    :data.expireAt,
                    :data.watchdogTimestamp
                )
                ON CONFLICT (id) DO NOTHING        
                """,
                minimumAffectedRows = 0
            )

            data class MarkJobAsInProgress(
                val id: String,
                val timestamp: Instant,
                val toState: Int = JobState.InProgress.id,
                val watchdogOwner: String
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    watchdog_timestamp = :timestamp,
                    watchdog_owner = :watchdogOwner
                WHERE state = ${JobState.Scheduled.id}
                  AND id = :id
                """.trimIndent()
            )

            data class MarkJobAsSuccessful(
                val id: String,
                val timestamp: Instant,
                val expireAt: Instant,
                val toState: Int = JobState.Successful.id,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    expire_at = :expireAt,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${JobState.InProgress.id}
                  AND id = :id
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent()
            )

            data class MarkJobAsFailed(
                val id: String,
                val timestamp: Instant,
                val expireAt: Instant,
                val toState: Int = JobState.Failed.id,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    expire_at = :expireAt,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${JobState.InProgress.id}
                  AND id = :id
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent()
            )

            data class MarkJobAsScheduled(
                val id: String,
                val timestamp: Instant,
                val scheduledFor: Instant,
                val toState: Int = JobState.Scheduled.id,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = :toState,
                    state_changed_at = :timestamp,
                    scheduled_for = :scheduledFor,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${JobState.InProgress.id}
                  AND id = :id
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent()
            )

            data class MarkStaleJobsAsScheduled(
                val timestamp: Instant,
                val staleIfWatchdogOlderThan: Instant,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET state = ${JobState.Scheduled.id},
                    state_changed_at = :timestamp,
                    scheduled_for = :timestamp,
                    watchdog_timestamp = NULL,
                    watchdog_owner = NULL
                WHERE state = ${JobState.InProgress.id}
                  AND watchdog_timestamp < :staleIfWatchdogOlderThan
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class UpdateWatchdog(
                val jobUid: Long,
                val watchdogTimestamp: Instant,
                val expectedWatchdogOwner: String,
            ) : TypedUpdate(
                """
                UPDATE $TableName
                SET watchdog_timestamp = :watchdogTimestamp
                WHERE uid = :jobUid
                  AND watchdog_owner = :expectedWatchdogOwner
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            data class DeleteExpiredJobs(
                val now: Instant,
            ) : TypedUpdate(
                "DELETE FROM $TableName WHERE expire_at < :now",
                minimumAffectedRows = 0
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
        val watchdogTimestamp: Instant?,
        val watchdogOwner: String?,
    )

    internal class StatisticsData(
        val count: Int,
        val stateId: Int
    )
}
