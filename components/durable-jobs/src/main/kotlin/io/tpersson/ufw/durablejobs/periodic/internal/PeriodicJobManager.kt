package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobStateData
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.Clock
import java.time.Duration

public class PeriodicJobManager @Inject constructor(
    private val periodicJobSpecsProvider: PeriodicJobSpecsProvider,
    private val periodicJobScheduler: PeriodicJobScheduler,
    private val periodicJobsDAO: PeriodicJobsDAO,
    private val clock: Clock,
) : ManagedJob() {

    private val schedulingInterval = Duration.ofSeconds(30) // TODO configurable

    public val periodicJobSpecs: List<PeriodicJobSpec<*>> get() = periodicJobSpecsProvider.periodicJobSpecs


    override suspend fun launch() {
        forever(logger, errorDelay = Duration.ofSeconds(5)) {
            runOnce()
            delay(schedulingInterval.toMillis())
        }
    }

    public suspend fun runOnce() {
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

