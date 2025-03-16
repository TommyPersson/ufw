package io.tpersson.ufw.durablejobs.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.durablejobs.*
import io.tpersson.ufw.durablejobs.DurableJob
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.InstantSource

@Singleton
public class DurableJobQueueImpl @Inject constructor(
    private val config: DurableJobsConfig,
    private val clock: InstantSource,
    private val workItemsDAO: WorkItemsDAO,
    private val workItemFailuresDAO: WorkItemFailuresDAO,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : DurableJobQueueInternal {

    private val logger = createLogger()

    override suspend fun <TJob : DurableJob> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: DurableJobOptionsBuilder.() -> Unit
    ) {
        val jobOptions = DurableJobOptionsBuilder().apply(builder)

        val jobDefinition = job::class.jobDefinition2

        val queueId = jobDefinition.queueId
        val type = jobDefinition.type

        // TODO WorkItemQueue abstraction to do signalling
        workItemsDAO.scheduleNewItem(
            newItem = NewWorkItem(
                queueId = queueId.toWorkItemQueueId(),
                type = type,
                itemId = job.id.toWorkItemId(),
                dataJson = objectMapper.writeValueAsString(job), // TODO move to new 'WorkItemsQueue'?
                metadataJson = "{}",
                concurrencyKey = null,
                scheduleFor = jobOptions.scheduleFor ?: clock.instant(),
            ),
            now = clock.instant(),
            unitOfWork = unitOfWork,
        )

        unitOfWork.addPostCommitHook {
            // TODO fix signalling for work items
            //getSignal(queueId).signal()
        }
    }

    override suspend fun getQueueStatistics(queueId: DurableJobQueueId): JobQueueStatistics {
        val workItemQueueStatistics = workItemsDAO.getQueueStatistics(queueId.toWorkItemQueueId())
        return JobQueueStatistics(
            queueId = queueId,
            numScheduled = workItemQueueStatistics.numScheduled,
            numPending = workItemQueueStatistics.numPending,
            numInProgress = workItemQueueStatistics.numInProgress,
            numFailed = workItemQueueStatistics.numFailed,
        )
    }

    override suspend fun getJobs(queueId: DurableJobQueueId, state: WorkItemState, paginationOptions: PaginationOptions): PaginatedList<WorkItemDbEntity> {
        return workItemsDAO.listAllItems(state = state, paginationOptions = paginationOptions)
    }

    override suspend fun getJob(queueId: DurableJobQueueId, jobId: DurableJobId): WorkItemDbEntity? {
        return workItemsDAO.getById(queueId.toWorkItemQueueId(), jobId.toWorkItemId())
    }

    override suspend fun rescheduleAllFailedJobs(queueId: DurableJobQueueId) {
        workItemsDAO.rescheduleAllFailedItems(queueId.toWorkItemQueueId(), clock.instant())
    }

    override suspend fun deleteAllFailedJobs(queueId: DurableJobQueueId) {
        workItemsDAO.deleteAllFailedItems(queueId.toWorkItemQueueId())
    }

    override suspend fun getJobFailures(
        queueId: DurableJobQueueId,
        jobId: DurableJobId,
        paginationOptions: PaginationOptions
    ): PaginatedList<WorkItemFailureDbEntity> {
        val workItem = workItemsDAO.getById(queueId.toWorkItemQueueId(), jobId.toWorkItemId())
            ?: return PaginatedList.empty(paginationOptions)

        return workItemFailuresDAO.listFailuresForWorkItem(workItem.uid, paginationOptions)
    }
}
