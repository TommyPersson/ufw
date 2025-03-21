package io.tpersson.ufw.durablejobs.admin.contracts

import io.tpersson.ufw.databasequeue.WorkQueueState
import java.time.Instant

public class JobQueueStatusDTO(
    public val state: WorkQueueState,
    public val stateChangedAt: Instant,
)