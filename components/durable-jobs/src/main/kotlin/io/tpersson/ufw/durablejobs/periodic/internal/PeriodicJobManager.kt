package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobStateData
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Clock
import java.time.Duration
import java.time.Instant

public class PeriodicJobManager @Inject constructor(
    private val periodicJobSpecsProvider: PeriodicJobSpecsProvider,
    private val periodicJobScheduler: PeriodicJobScheduler,
    private val periodicJobsDAO: PeriodicJobsDAO,
    private val clock: Clock,
) : ManagedPeriodicTask(
    interval = Duration.ofSeconds(30) // TODO configurable
) {
    public val periodicJobSpecs: List<PeriodicJobSpec<*>> get() = periodicJobSpecsProvider.periodicJobSpecs

    public override suspend fun runOnce() {
        periodicJobScheduler.scheduleAnyPendingJobs()
    }

    public suspend fun getState(): List<PeriodicJobStateData> {
        return periodicJobsDAO.getAll(PaginationOptions.DEFAULT).items
    }

    public suspend fun scheduleJobNow(
        periodicJobSpec: PeriodicJobSpec<*>,
        now: Instant = clock.instant(),
    ): DurableJobId {
        return periodicJobScheduler.scheduleJobNow(periodicJobSpec, now)
    }
}

