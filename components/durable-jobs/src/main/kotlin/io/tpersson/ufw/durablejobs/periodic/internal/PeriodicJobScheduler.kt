package io.tpersson.ufw.durablejobs.periodic.internal

import java.time.Instant

public interface PeriodicJobScheduler {
    public suspend fun scheduleAnyPendingJobs()

    public suspend fun scheduleJobNow(
        periodicJobSpec: PeriodicJobSpec<*>,
        now: Instant,
    )
}