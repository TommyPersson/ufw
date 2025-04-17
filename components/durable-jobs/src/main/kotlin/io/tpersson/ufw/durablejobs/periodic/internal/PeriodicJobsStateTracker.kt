package io.tpersson.ufw.durablejobs.periodic.internal

import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.core.utils.forever
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.WorkItemStateChange
import io.tpersson.ufw.databasequeue.WorkQueue
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.internal.toWorkItemQueueId
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.managed.ManagedJob
import jakarta.inject.Inject
import java.time.Duration

// TODO tests
public class PeriodicJobsStateTracker @Inject constructor(
    private val workQueue: WorkQueue,
    private val periodicJobSpecsProvider: PeriodicJobSpecsProvider,
    private val periodicJobsDAO: PeriodicJobsDAO,
    private val unitOfWorkFactory: UnitOfWorkFactory,
) : ManagedJob() {

    private val specs by Memoized({ periodicJobSpecsProvider.periodicJobSpecs }) { it }

    override suspend fun launch() {
        forever(logger, errorDelay = Duration.ofSeconds(5)) {
            workQueue.stateChanges.collect(::onStateChange)
        }
    }

    private suspend fun onStateChange(change: WorkItemStateChange) {
        val matchingSpec = specs.firstOrNull { it.matches(change) }
            ?: return

        unitOfWorkFactory.use { uow ->
            periodicJobsDAO.setExecutionInfo(
                queueId = matchingSpec.handler.jobDefinition.queueId,
                jobType = matchingSpec.handler.jobDefinition.type,
                state = change.toState,
                stateChangeTimestamp = change.timestamp,
                unitOfWork = uow,
            )
        }
    }

    private fun PeriodicJobSpec<*>.matches(change: WorkItemStateChange): Boolean {
        val queueId = handler.jobDefinition.queueId.toWorkItemQueueId()
        val itemType = handler.jobDefinition.type

        return queueId == change.queueId && change.itemType == itemType
    }
}