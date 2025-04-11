package io.tpersson.ufw.durablejobs.periodic.internal.dao

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import jakarta.inject.Inject
import java.time.Instant

public class PeriodicJobsDAOImpl @Inject constructor(
    private val database: Database
) : PeriodicJobsDAO {
    override suspend fun get(queueId: DurableJobQueueId, jobType: String): PeriodicJobStateData? {
        return database.select(Queries.Selects.Get(queueId, jobType))
    }

    override suspend fun setSchedulingInfo(
        queueId: DurableJobQueueId,
        jobType: String,
        nextSchedulingAttempt: Instant?,
        lastSchedulingAttempt: Instant?,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(Queries.Updates.SetSchedulingInfo(queueId, jobType, nextSchedulingAttempt, lastSchedulingAttempt))
    }

    override suspend fun setExecutionInfo(
        queueId: DurableJobQueueId,
        jobType: String,
        state: WorkItemState?,
        stateChangeTimestamp: Instant?,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(Queries.Updates.SetExecutionInfo(queueId, jobType, state, stateChangeTimestamp))
    }

    override suspend fun debugTruncate() {
        database.update(Queries.Updates.DebugTruncate)
    }

    private object Queries {

        private const val tableName = "ufw__periodic_jobs"

        object Selects {
            data class Get(
                val queueId: DurableJobQueueId,
                val jobType: String,
            ) : TypedSelectSingle<PeriodicJobStateData>(
                """
                SELECT * 
                FROM $tableName 
                WHERE queue_id = :queueId.value 
                  AND job_type = :jobType
                """.trimIndent()
            )
        }

        object Updates {
            data class Put(
                val queueId: DurableJobQueueId,
                val jobType: String,
                val data: PeriodicJobStateData,
            ) : TypedUpdate(
                """
                INSERT INTO $tableName AS t (
                    queue_id,
                    job_type,
                    last_scheduling_attempt,
                    next_scheduling_attempt
                ) VALUES (
                    :queueId.value,
                    :jobType,
                    :data.lastSchedulingAttempt,
                    :data.nextSchedulingAttempt
                ) ON CONFLICT (queue_id, job_type) DO UPDATE
                    SET last_scheduling_attempt = :data.lastSchedulingAttempt,
                        next_scheduling_attempt = :data.nextSchedulingAttempt                """
            )

            data class SetSchedulingInfo(
                val queueId: DurableJobQueueId,
                val jobType: String,
                val nextSchedulingAttempt: Instant?,
                val lastSchedulingAttempt: Instant?,
            ) : TypedUpdate(
                """
                INSERT INTO $tableName AS t (
                    queue_id,
                    job_type,
                    last_scheduling_attempt,
                    next_scheduling_attempt
                ) VALUES (
                    :queueId.value,
                    :jobType,
                    :lastSchedulingAttempt,
                    :nextSchedulingAttempt
                ) ON CONFLICT (queue_id, job_type) DO UPDATE
                    SET last_scheduling_attempt = :lastSchedulingAttempt,
                        next_scheduling_attempt = :nextSchedulingAttempt           
                """
            )

            data class SetExecutionInfo(
                val queueId: DurableJobQueueId,
                val jobType: String,
                val state: WorkItemState?,
                val stateChangeTimestamp: Instant?,
            ) : TypedUpdate(
                """
                INSERT INTO $tableName AS t (
                    queue_id,
                    job_type,
                    last_execution_state,
                    last_execution_state_change_timestamp
                ) VALUES (
                    :queueId.value,
                    :jobType,
                    :state.dbOrdinal,
                    :stateChangeTimestamp
                ) ON CONFLICT (queue_id, job_type) DO UPDATE
                    SET last_execution_state = :state.dbOrdinal,
                        last_execution_state_change_timestamp = :stateChangeTimestamp           
                """
            )

            @Suppress("SqlWithoutWhere")
            object DebugTruncate : TypedUpdate(
                "DELETE FROM $tableName",
                minimumAffectedRows = 0
            )
        }
    }
}