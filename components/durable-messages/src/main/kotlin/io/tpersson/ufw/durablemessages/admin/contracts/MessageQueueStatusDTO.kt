package io.tpersson.ufw.durablemessages.admin.contracts

import io.tpersson.ufw.databasequeue.WorkQueueState
import java.time.Instant

public class MessageQueueStatusDTO(
    public val state: WorkQueueState,
    public val stateChangedAt: Instant,
)