package io.tpersson.ufw.examples.common.managed

import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.examples.common.jobs.PrintJob2
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

public class PeriodicJobScheduler @Inject constructor(
    private val jobQueue: DurableJobQueue,
    private val featureToggles: FeatureToggles,
    private val unitOfWorkFactory: UnitOfWorkFactory,
) : ManagedJob() {

    private val toggleHandle = featureToggles.get("PeriodicJobScheduler")

    override suspend fun launch(): Unit = coroutineScope {
        var i = 0

        forever(logger) {
            i++

            withContext(NonCancellable) {
                delay(500)
                runOnce(i)
            }
        }
    }

    private suspend fun runOnce(i: Int) {
        if (!toggleHandle.isEnabled()) {
            return
        }

        unitOfWorkFactory.use { uow ->
            val job = PrintJob2("$i")
            jobQueue.enqueue(job, uow)
        }
    }
}