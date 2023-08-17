package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedSelectSingle
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject

public class JobFailureRepositoryImpl @Inject constructor(
    private val database: Database
) : JobFailureRepository {

    override suspend fun getLatestFor(job: InternalJob<*>, limit: Long): List<JobFailure> {
        return database.select(Queries.Selects.GetLatestFor(job.uid!!, limit))
    }

    override suspend fun getNumberOfFailuresFor(job: InternalJob<*>): Int {
        return database.select(Queries.Selects.GetNumberOfFailuresFor(job.uid!!))?.count ?: 0
    }

    override suspend fun insert(failure: JobFailure, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.Insert(failure))
    }

    override suspend fun debugTruncate(unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.DebugTruncate)
    }

    public data class Count(val count: Int)

    private object Queries {
        val TableName = "ufw__job_queue__failures"

        object Selects {
            class GetNumberOfFailuresFor(
                val jobUid: Long
            ) : TypedSelectSingle<Count>(
                """
                SELECT COUNT(*) AS count
                FROM $TableName
                WHERE job_uid = :jobUid                   
                """.trimIndent()
            )

            class GetLatestFor(
                val jobUid: Long,
                val limit: Long,
            ) : TypedSelectList<JobFailure>(
                """
                    SELECT *
                    FROM $TableName
                    WHERE job_uid = :jobUid
                    ORDER BY timestamp DESC
                    LIMIT :limit
                """.trimIndent()
            )
        }

        object Updates {
            class Insert(val data: JobFailure) : TypedUpdate(
                """
                    INSERT INTO $TableName (
                        id,
                        job_uid,
                        timestamp, 
                        error_type,
                        error_message, 
                        error_stack_trace
                    ) VALUES (
                        :data.id::text,
                        :data.jobUid,
                        :data.timestamp, 
                        :data.errorType,
                        :data.errorMessage, 
                        :data.errorStackTrace
                    ) 
                """.trimIndent()
            )

            object DebugTruncate : TypedUpdate("DELETE FROM $TableName", minimumAffectedRows = 0)
        }
    }
}