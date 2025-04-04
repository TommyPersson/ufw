package io.tpersson.ufw.durableevents.handler.internal

import io.tpersson.ufw.databasequeue.WorkItemId
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.convertQueueId
import io.tpersson.ufw.durableevents.common.DurableEventId
import io.tpersson.ufw.durableevents.common.DurableEventQueueId


public fun DurableEventQueueId.toWorkItemQueueId(): WorkItemQueueId =
    DurableEventsDatabaseQueueAdapterSettings.convertQueueId(this.id)

public fun DurableEventId.toWorkItemId(): WorkItemId = WorkItemId(this.value)
