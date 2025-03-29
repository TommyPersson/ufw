package io.tpersson.ufw.durableevents.admin.contracts

import io.tpersson.ufw.databasequeue.WorkQueueState
import java.time.Instant

public class EventQueueStatusDTO(
    public val state: WorkQueueState,
    public val stateChangedAt: Instant,
)