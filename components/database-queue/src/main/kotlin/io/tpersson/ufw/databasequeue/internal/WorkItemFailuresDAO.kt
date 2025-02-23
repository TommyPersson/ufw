package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject

public interface WorkItemFailuresDAO {
    public fun saveFailure(
        failure: WorkItemFailureDbEntity,
        unitOfWork: UnitOfWork,
    )
}

public class WorkItemFailuresDAOImpl @Inject constructor(
    private val database: Database,
) : WorkItemFailuresDAO {

    override fun saveFailure(failure: WorkItemFailureDbEntity, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.InsertFailure(failure))
    }

    private object Queries {
        val TableName = "ufw__db_queue__failures"

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
        }
    }

}