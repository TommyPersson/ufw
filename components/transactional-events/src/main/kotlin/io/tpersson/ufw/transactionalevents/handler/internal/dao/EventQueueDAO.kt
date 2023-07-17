package io.tpersson.ufw.transactionalevents.handler.internal.dao

import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork

public class EventQueueDAO {
    public fun insert(event: EventEntityData, unitOfWork: UnitOfWork) {
        unitOfWork.add(Queries.Updates.Insert(event))
    }

    internal object Queries {
        object Updates {
            class Insert(val data: EventEntityData) : TypedUpdate(
                """
                INSERT INTO ufw__transactional_events__queue (
                    queue_id, 
                    id, 
                    topic, 
                    type, 
                    data_json, 
                    ce_data_json, 
                    timestamp,
                    state,
                    created_at, 
                    scheduled_for, 
                    state_changed_at, 
                    watchdog_timestamp, 
                    watchdog_owner, 
                    expire_at
                ) VALUES (
                    :data.queueId, 
                    :data.id, 
                    :data.topic, 
                    :data.type, 
                    :data.dataJson::jsonb, 
                    :data.ceDataJson::jsonb, 
                    :data.timestamp, 
                    :data.state,
                    :data.createdAt, 
                    :data.scheduledFor, 
                    :data.stateChangedAt, 
                    :data.watchdogTimestamp, 
                    :data.watchdogOwner, 
                    :data.expireAt
                ) ON CONFLICT (queue_id, id) DO NOTHING 
                """.trimIndent(),
                minimumAffectedRows = 0
            )
        }
    }
}

