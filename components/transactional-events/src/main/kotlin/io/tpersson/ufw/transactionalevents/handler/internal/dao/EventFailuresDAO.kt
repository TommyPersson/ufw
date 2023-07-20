package io.tpersson.ufw.transactionalevents.handler.internal.dao

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.transactionalevents.handler.internal.EventFailure
import jakarta.inject.Inject

public class EventFailuresDAO @Inject constructor(
    private val database: Database
) {
    public suspend fun getNumberOfFailuresFor(eventUid: Long): Int {
        return database.select(Queries.Selects.GetCount(eventUid))?.count ?: 0
    }

    public fun insert(failure: EventFailure, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.Insert(failure))
    }

    internal object Queries {
        const val TableName = "ufw__transactional_events__failures"

        data class CountResult(val count: Int)

        object Selects {
            data class GetCount(
                val eventUid: Long
            ) : TypedSelect<CountResult>(
                """
                SELECT count(*) as count 
                FROM $TableName
                WHERE event_uid = :eventUid
                """.trimIndent()
            )
        }

        object Updates {
            data class Insert(
                val data: EventFailure
            ) : TypedUpdate(
                """
                INSERT INTO $TableName (
                    id,
                    event_uid,
                    timestamp,
                    error_type,
                    error_message,
                    error_stack_trace
                ) VALUES (
                    :data.id,
                    :data.eventUid,
                    :data.timestamp,
                    :data.errorType,
                    :data.errorMessage,
                    :data.errorStackTrace
                )  
                """.trimIndent()
            )
        }
    }
}