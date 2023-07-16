package io.tpersson.ufw.transactionalevents.publisher.internal.dao

import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import jakarta.inject.Inject

public class EventOutboxDAO @Inject constructor(
    private val database: Database,
) {
    public fun insertAll(events: List<EventEntityData>, unitOfWork: UnitOfWork) {
        for (event in events) {
            unitOfWork.add(Queries.Updates.Insert(event))
        }
    }

    public suspend fun getNextBatch(limit: Int): List<EventEntityData> {
        return database.selectList(Queries.Selects.GetNextBatch(limit))
    }

    public fun deleteBatch(uids: List<Long>, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.DeleteBatch(uids))
    }

    @Suppress("unused")
    internal object Queries {
        object Updates {
            class Insert(
                val data: EventEntityData
            ) : TypedUpdate(
                """
                INSERT INTO ufw__transactional_events__outbox (
                    id, 
                    topic, 
                    type, 
                    data_json, 
                    ce_data_json, 
                    timestamp
                ) VALUES (
                    :data.id,
                    :data.topic,
                    :data.type,
                    :data.dataJson::jsonb,
                    :data.ceDataJson::jsonb,
                    :data.timestamp
                )                
                """.trimIndent())

            class DeleteBatch(
                val uids: List<Long>
            ) : TypedUpdate(
                """
                DELETE FROM ufw__transactional_events__outbox
                WHERE uid = ANY(:uids)
                """.trimIndent()
            )
        }

        object Selects {
            class GetNextBatch(
                val limit: Int,
            ) : TypedSelect<EventEntityData>(
                """
                SELECT * FROM ufw__transactional_events__outbox 
                ORDER BY uid 
                LIMIT :limit
                """.trimIndent()
            )
        }
    }
}

