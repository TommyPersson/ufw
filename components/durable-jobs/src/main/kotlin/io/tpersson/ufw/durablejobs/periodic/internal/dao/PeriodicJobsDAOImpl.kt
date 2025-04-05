package io.tpersson.ufw.durablejobs.periodic.internal.dao

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import jakarta.inject.Inject

public class PeriodicJobsDAOImpl @Inject constructor(
    private val database: Database
) : PeriodicJobsDAO {
    override suspend fun get(queueId: DurableJobQueueId, jobType: String): PeriodicJobStateData? {
        return database.select(Queries.Selects.Get(queueId, jobType))
    }

    override suspend fun put(
        queueId: DurableJobQueueId,
        jobType: String,
        state: PeriodicJobStateData,
        unitOfWork: UnitOfWork
    ) {
        unitOfWork.add(Queries.Updates.Put(queueId, jobType, state))
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
                INSERT INTO $tableName (
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
                        next_scheduling_attempt = :data.nextSchedulingAttempt 
                """
            )
        }
    }
}