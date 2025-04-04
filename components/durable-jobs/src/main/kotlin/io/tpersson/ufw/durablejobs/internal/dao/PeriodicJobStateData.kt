package io.tpersson.ufw.durablejobs.internal.dao

import java.time.Instant

public data class PeriodicJobStateData(
    val queueId: String,
    val jobType: String,
    val nextSchedulingAttempt: Instant?,
    val lastSchedulingAttempt: Instant?
)