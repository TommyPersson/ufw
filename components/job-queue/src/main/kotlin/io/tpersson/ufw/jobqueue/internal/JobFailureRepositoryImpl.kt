package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.useInTransaction
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.typedqueries.selectList
import io.tpersson.ufw.database.typedqueries.selectSingle
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject

public class JobFailureRepositoryImpl @Inject constructor(
    private val connectionProvider: ConnectionProvider
) : JobFailureRepository {

    override suspend fun getLatestFor(job: InternalJob<*>, limit: Long): List<JobFailure> {
        return connectionProvider.get().useInTransaction {
            it.selectList(Queries.Selects.GetLatestFor(job.uid!!, limit))
        } ?: emptyList()
    }

    override suspend fun getNumberOfFailuresFor(job: InternalJob<*>): Int {
        return connectionProvider.get().useInTransaction {
            it.selectSingle(Queries.Selects.GetNumberOfFailuresFor(job.uid!!))
        }?.count ?: 0
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
            ) : TypedSelect<Count>(
                """
                SELECT COUNT(*) AS count
                FROM ufw__job_queue__failures
                WHERE job_uid = :jobUid                   
                """.trimIndent()
            )

            class GetLatestFor(
                val jobUid: Long,
                val limit: Long,
            ) : TypedSelect<JobFailure>(
                """
                    SELECT *
                    FROM ufw__job_queue__failures
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