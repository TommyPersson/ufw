package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.databasequeue.WorkItemId
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.convertQueueId
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId


public fun DurableMessageQueueId.toWorkItemQueueId(): WorkItemQueueId =
    DurableMessagesDatabaseQueueAdapterSettings.convertQueueId(this.id)

public fun DurableMessageId.toWorkItemId(): WorkItemId = WorkItemId(this.value)
