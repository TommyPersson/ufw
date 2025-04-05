package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.InstantSource

public class PeriodicJobManager @Inject constructor(
    private val periodicJobSpecsProvider: PeriodicJobSpecsProvider,
    private val periodicJobScheduler: PeriodicJobScheduler,
    private val periodicJobsDAO: PeriodicJobsDAO,
    private val clock: InstantSource,
) : ManagedJob() {

    public val periodicJobSpecs: List<PeriodicJobSpec<*>> get() = periodicJobSpecsProvider.periodicJobSpecs

    override suspend fun launch() {
        forever(logger) {
            periodicJobScheduler.scheduleAnyPendingJobs()

            delay(1_000)
        }
    }

    public suspend fun getState(spec: PeriodicJobSpec<*>): PeriodicJobState {
        return periodicJobsDAO.get(
            queueId = spec.handler.jobDefinition.queueId,
            jobType = spec.handler.jobDefinition.type
        )?.let {
            PeriodicJobState(
                lastSchedulingAttempt = it.lastSchedulingAttempt,
                nextSchedulingAttempt = it.nextSchedulingAttempt
            )
        } ?: PeriodicJobState()
    }

    public suspend fun scheduleJobNow(
        periodicJobSpec: PeriodicJobSpec<*>,
        now: Instant = clock.instant(),
    ) {
        periodicJobScheduler.scheduleJobNow(periodicJobSpec, now)
    }
}

