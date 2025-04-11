package io.tpersson.ufw.durablejobs.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.databasequeue.NewWorkItem
import io.tpersson.ufw.databasequeue.WorkQueue
import io.tpersson.ufw.durablejobs.DurableJob
import io.tpersson.ufw.durablejobs.DurableJobOptionsBuilder
import io.tpersson.ufw.durablejobs.DurableJobsConfig
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.InstantSource

@Singleton
public class DurableJobQueueImpl @Inject constructor(
    private val config: DurableJobsConfig,
    private val clock: InstantSource,
    private val workQueue: WorkQueue,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : DurableJobQueueInternal {

    override suspend fun <TJob : DurableJob> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: DurableJobOptionsBuilder.() -> Unit
    ) {
        val jobOptions = DurableJobOptionsBuilder().apply(builder)

        val jobDefinition = job::class.jobDefinition2

        val queueId = jobDefinition.queueId
        val type = jobDefinition.type

        workQueue.schedule(
            item = NewWorkItem(
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
    }
}
