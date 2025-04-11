package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.databasequeue.WorkItemState
import java.time.Instant

public data class PeriodicJobState(
    val lastSchedulingAttempt: Instant? = null,
    val nextSchedulingAttempt: Instant? = null,
    val lastExecutionState: WorkItemState? = null,
    val lastExecutionStateChangeTimestamp: Instant? = null,
)