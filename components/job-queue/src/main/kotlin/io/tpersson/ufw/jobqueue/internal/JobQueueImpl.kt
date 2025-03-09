package io.tpersson.ufw.jobqueue.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.concurrency.ConsumerSignal
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.jobqueue.*
import io.tpersson.ufw.jobqueue.DurableJob
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.time.withTimeoutOrNull
import java.lang.Exception
import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Singleton
public class JobQueueImpl @Inject constructor(
    private val config: JobQueueConfig,
    private val clock: InstantSource,
    private val workItemsDAO: WorkItemsDAO,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : JobQueueInternal {

    private val logger = createLogger()

    override suspend fun <TJob : DurableJob> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit
    ) {
        val jobOptions = JobOptionsBuilder().apply(builder)

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
            //getSignal(queueId).signal()
        }
    }

    override suspend fun getQueueStatistics(queueId: JobQueueId): JobQueueStatistics {
        val workItemQueueStatistics = workItemsDAO.getQueueStatistics(queueId.toWorkItemQueueId())
        return JobQueueStatistics(
            queueId = queueId,
            numScheduled = workItemQueueStatistics.numScheduled,
            numPending = workItemQueueStatistics.numPending,
            numInProgress = workItemQueueStatistics.numInProgress,
            numFailed = workItemQueueStatistics.numFailed,
        )
    }
}
