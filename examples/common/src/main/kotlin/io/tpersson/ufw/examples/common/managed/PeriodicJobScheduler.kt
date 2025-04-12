package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.examples.common.featuretoggles.AppFeatureToggles
import io.tpersson.ufw.examples.common.jobs.ExpensiveCalculationJob
import io.tpersson.ufw.examples.common.jobs.PrintJob2
import io.tpersson.ufw.examples.common.jobs.SensitiveDataRefreshJob
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.managed.ManagedPeriodicTask
import jakarta.inject.Inject
import java.time.Duration

public class PeriodicJobScheduler @Inject constructor(
    private val jobQueue: DurableJobQueue,
    private val featureToggles: FeatureToggles,
    private val unitOfWorkFactory: UnitOfWorkFactory,
) : ManagedPeriodicTask(
    interval = Duration.ofMillis(500)
) {

    private val featureToggleHandle = featureToggles.get(AppFeatureToggles.PeriodicJobScheduler)

    private var i = 0

    override suspend fun runOnce() {
        i++

        if (!featureToggleHandle.isEnabled()) {
            return
        }

        unitOfWorkFactory.use { uow ->
            jobQueue.enqueue(PrintJob2("$i"), uow)
            jobQueue.enqueue(ExpensiveCalculationJob(), uow)
            jobQueue.enqueue(SensitiveDataRefreshJob(), uow)
        }
    }
}