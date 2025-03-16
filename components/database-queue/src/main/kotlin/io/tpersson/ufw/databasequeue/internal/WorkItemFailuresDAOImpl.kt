package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject

public class WorkItemFailuresDAOImpl @Inject constructor(
    private val database: Database,
) : WorkItemFailuresDAO {

    override fun insertFailure(failure: WorkItemFailureDbEntity, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.InsertFailure(failure))
    }

    override suspend fun debugTruncate() {
        database.update(Queries.Updates.DebugTruncate)
    }

    override suspend fun listFailuresForWorkItem(
        itemUid: Long,
        paginationOptions: PaginationOptions,
    ): PaginatedList<WorkItemFailureDbEntity> {
        return database.select(Queries.Selects.ListFailuresForWorkItem(itemUid, paginationOptions))
    }

    private object Queries {
        val TableName = "ufw__db_queue__failures"

        object Selects {
            data class ListFailuresForWorkItem(
                val itemUid: Long,
                override val paginationOptions: PaginationOptions,
            ) : TypedSelectList<WorkItemFailureDbEntity>(
                """
                SELECT * 
                FROM $TableName 
                WHERE item_uid = :itemUid
                ORDER BY timestamp DESC
                """.trimIndent()
            )
        }

        object Updates {
            data class InsertFailure(
                val failure: WorkItemFailureDbEntity
            ) : TypedUpdate(
                """
                    INSERT INTO $TableName (
                        id,
                        item_uid,
                        timestamp,
                        error_type,
                        error_message,
                        error_stack_trace
                    ) VALUES (
                        :failure.id,
                        :failure.itemUid,
                        :failure.timestamp,
                        :failure.errorType,
                        :failure.errorMessage,
                        :failure.errorStackTrace
                    ) ON CONFLICT (id) DO NOTHING
                """,
                minimumAffectedRows = 0
            )

            data object DebugTruncate : TypedUpdate(
                """DELETE FROM $TableName""",
                minimumAffectedRows = 0
            )
        }
    }

}