package io.tpersson.ufw.durablejobs.periodic.internal

import java.time.Instant

public data class PeriodicJobState(
    val lastSchedulingAttempt: Instant? = null,
    val nextSchedulingAttempt: Instant? = null,
)