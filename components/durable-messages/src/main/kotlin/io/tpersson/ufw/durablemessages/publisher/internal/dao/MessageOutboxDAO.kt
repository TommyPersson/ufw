package io.tpersson.ufw.durablemessages.publisher.internal.dao

import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.core.utils.paginate
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.typedqueries.TypedSelectList
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import jakarta.inject.Inject
import kotlinx.coroutines.flow.toList

public class MessageOutboxDAO @Inject constructor(
    private val database: Database,
) {
    public fun insertAll(messages: List<MessageEntityData>, unitOfWork: UnitOfWork) {
        for (message in messages) {
            unitOfWork.add(Queries.Updates.Insert(message))
        }
    }

    public suspend fun getNextBatch(limit: Int): List<MessageEntityData> {
        val paginationOptions = PaginationOptions.DEFAULT.copy(limit = limit)
        return paginate(paginationOptions) {
            database.select(Queries.Selects.GetNextBatch(it))
        }.toList().flatMap { it.items }
    }

    public fun deleteBatch(uids: List<Long>, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.DeleteBatch(uids))
    }

    @Suppress("unused")
    internal object Queries {
        object Updates {
            class Insert(
                val data: MessageEntityData
            ) : TypedUpdate(
                """
                INSERT INTO ufw__durable_messages__outbox (
                    id, 
                    topic, 
                    type, 
                    data_json, 
                    metadata_json, 
                    timestamp
                ) VALUES (
                    :data.id,
                    :data.topic,
                    :data.type,
                    :data.dataJson::jsonb,
                    :data.metadataJson::jsonb,
                    :data.timestamp
                ) ON CONFLICT (id) DO NOTHING              
                """.trimIndent(),
                minimumAffectedRows = 0
            )

            class DeleteBatch(
                val uids: List<Long>
            ) : TypedUpdate(
                """
                DELETE FROM ufw__durable_messages__outbox
                WHERE uid = ANY(:uids)
                """.trimIndent()
            )
        }

        object Selects {
            class GetNextBatch(
                override val paginationOptions: PaginationOptions,
            ) : TypedSelectList<MessageEntityData>(
                """
                SELECT * FROM ufw__durable_messages__outbox 
                ORDER BY uid
                """.trimIndent()
            )
        }
    }
}

