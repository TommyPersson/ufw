package io.tpersson.ufw.durablejobs.periodic.internal.dao

import java.time.Instant

public data class PeriodicJobStateData(
    val queueId: String,
    val jobType: String,
    val nextSchedulingAttempt: Instant? = null,
    val lastSchedulingAttempt: Instant? = null,
    val lastExecutionState: Int? = null,
    val lastExecutionStateChangeTimestamp: Instant? = null,
)