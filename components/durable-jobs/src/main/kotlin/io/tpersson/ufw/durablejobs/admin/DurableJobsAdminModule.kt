package io.tpersson.ufw.durablejobs.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueInternal
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import io.tpersson.ufw.durablejobs.internal.DurableJobDefinition
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant

public class DurableJobsAdminModule @Inject constructor(
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    private val jobQueue: DurableJobQueueInternal
) : AdminModule {

    private val logger = createLogger()

    public override val moduleId: String = "durable-jobs"

    private val jobHandlerDefinitions: List<DurableJobDefinition<*>> by lazy {
        durableJobHandlersProvider.get()
            .map { it.jobDefinition }
            .sortedBy { it.queueId }
    }

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/job-queue/hello") {
                call.respondText("Hello, Jobs!")
            }

            get("/admin/api/job-queue/queues") {
                coroutineScope {
                    val queueIds = jobHandlerDefinitions.map { it.queueId }
                    val listItems = queueIds.map { queueId ->
                        async {
                            val stats = jobQueue.getQueueStatistics(queueId)
                            QueueListItemDTO(
                                queueId = queueId,
                                numScheduled = stats.numScheduled,
                                numPending = stats.numPending,
                                numInProgress = stats.numInProgress,
                                numFailed = stats.numFailed,
                            )
                        }
                    }.awaitAll()

                    call.respond(listItems)
                }
            }

            get("/admin/api/job-queue/queues/{queueId}/details") {
                val queueId = DurableJobQueueId.fromString(call.parameters["queueId"]!!)

                val handlers = jobHandlerDefinitions.filter { it.queueId == queueId }

                val stats = jobQueue.getQueueStatistics(queueId)

                val details = QueueDetailsDTO(
                    queueId = queueId,
                    numScheduled = stats.numScheduled,
                    numPending = stats.numPending,
                    numInProgress = stats.numInProgress,
                    numFailed = stats.numFailed,
                    jobTypes = handlers.map {
                        QueueDetailsDTO.JobType(
                            type = it.type,
                            jobClassName = it.jobClass.simpleName!!,
                            description = it.description
                        )
                    }
                )

                call.respond(details)
            }

            post("/admin/api/job-queue/queues/{queueId}/actions/reschedule-all-failed-jobs") {
                val queueId = DurableJobQueueId.fromString(call.parameters["queueId"]!!)

                jobQueue.rescheduleAllFailedJobs(queueId)

                logger.info("Rescheduled all failed jobs for queue: $queueId")

                call.respond(HttpStatusCode.NoContent)
            }


            get("/admin/api/job-queue/queues/{queueId}/jobs") {
                val queueId = DurableJobQueueId.fromString(call.parameters["queueId"]!!)
                val jobState = WorkItemState.fromString(call.parameters["state"]!!)

                val paginationOptions = call.getPaginationOptions()

                val paginatedJobs = jobQueue.getJobs(queueId, jobState, paginationOptions)

                val jobList = PaginatedListDTO(
                    items = paginatedJobs.items.map {
                        JobItemDTO(
                            jobId = it.itemId,
                            numFailures = it.numFailures,
                            createdAt = it.createdAt,
                            firstScheduledFor = it.firstScheduledFor,
                            nextScheduledFor = it.nextScheduledFor,
                            stateChangedAt = it.stateChangedAt,
                        )
                    },
                    hasMoreItems = paginatedJobs.hasMoreItems,
                )

                call.respond(jobList)
            }
        }
    }
}

public fun ApplicationCall.getPaginationOptions(): PaginationOptions {
    val limit = parameters["limit"]?.toInt() ?: 100
    val offset = parameters["offset"]?.toInt() ?: 0
    return PaginationOptions(limit, offset)
}

public data class PaginatedListDTO<TItem>(
    val items: List<TItem>,
    val hasMoreItems: Boolean,
)

public data class QueueListItemDTO(
    val queueId: DurableJobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
)

public data class QueueDetailsDTO(
    val queueId: DurableJobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
    val jobTypes: List<JobType>,
) {
    public data class JobType(
        val type: String,
        val jobClassName: String,
        val description: String?,
    )
}

public data class JobItemDTO(
    val jobId: String,
    val numFailures: Int,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
    val stateChangedAt: Instant,
)