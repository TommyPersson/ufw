package io.tpersson.ufw.durablejobs.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import io.tpersson.ufw.durablejobs.internal.DurableJobDefinition
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueInternal
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.time.InstantSource

public class DurableJobsAdminModule @Inject constructor(
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    private val jobQueue: DurableJobQueueInternal,
    private val clock: InstantSource
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
            get("/admin/api/durable-jobs/queues") {
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

            get("/admin/api/durable-jobs/queues/{queueId}/details") {
                val queueId = call.parameters.queueId!!

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

            post("/admin/api/durable-jobs/queues/{queueId}/actions/reschedule-all-failed-jobs") {
                val queueId = call.parameters.queueId!!

                jobQueue.rescheduleAllFailedJobs(queueId)

                logger.info("Rescheduled all failed jobs for queue: $queueId")

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/actions/delete-all-failed-jobs") {
                val queueId = call.parameters.queueId!!

                jobQueue.deleteAllFailedJobs(queueId)

                logger.info("Deleted all failed jobs for queue: $queueId")

                call.respond(HttpStatusCode.NoContent)
            }

            get("/admin/api/durable-jobs/queues/{queueId}/jobs") {
                val queueId = call.parameters.queueId!!
                val jobState = call.parameters.state!!

                val paginationOptions = call.getPaginationOptions()

                val paginatedJobs = jobQueue.getJobs(queueId, jobState, paginationOptions)

                val jobList = paginatedJobs.toDTO {
                    JobItemDTO(
                        jobId = it.itemId,
                        jobType = it.type,
                        createdAt = it.createdAt,
                        firstScheduledFor = it.firstScheduledFor,
                        nextScheduledFor = it.nextScheduledFor,
                        stateChangedAt = it.stateChangedAt,
                        numFailures = it.numFailures,
                    )
                }

                call.respond(jobList)
            }

            get("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/details") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                val job = jobQueue.getJob(queueId, jobId)?.let {
                    JobDetailsDTO(
                        queueId = queueId.value,
                        jobId = it.itemId,
                        jobType = it.type,
                        state = WorkItemState.fromDbOrdinal(it.state).name,
                        dataJson = it.dataJson,
                        metadataJson = it.metadataJson,
                        concurrencyKey = it.concurrencyKey,
                        createdAt = it.createdAt,
                        firstScheduledFor = it.firstScheduledFor,
                        nextScheduledFor = it.nextScheduledFor,
                        stateChangedAt = it.stateChangedAt,
                        watchdogTimestamp = it.watchdogTimestamp,
                        watchdogOwner = it.watchdogOwner,
                        numFailures = it.numFailures,
                        expiresAt = it.expiresAt,
                    )
                }

                if (job == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(job)
            }

            get("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/failures") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                val paginationOptions = call.getPaginationOptions(defaultLimit = 5)

                val failures = jobQueue.getJobFailures(queueId, jobId, paginationOptions).toDTO {
                    JobFailureDTO(
                        failureId = it.id,
                        jobId = jobId.value,
                        timestamp = it.timestamp,
                        errorType = it.errorType,
                        errorMessage = it.errorMessage,
                        errorStackTrace = it.errorStackTrace,
                    )
                }

                call.respond(failures)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/actions/delete") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                jobQueue.deleteFailedJob(queueId, jobId)

                logger.info("Deleted job: <$queueId/$jobId>")

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/actions/reschedule-now") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                jobQueue.rescheduleFailedJob(queueId, jobId, clock.instant())

                logger.info("Rescheduled job: <$queueId/$jobId>")

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private val Parameters.queueId: DurableJobQueueId? get() = this["queueId"]?.let { DurableJobQueueId.fromString(it) }
private val Parameters.jobId: DurableJobId? get() = this["jobId"]?.let { DurableJobId.fromString(it) }
private val Parameters.state: WorkItemState? get() = this["state"]?.let { WorkItemState.fromString(it) }

public fun ApplicationCall.getPaginationOptions(
    defaultLimit: Int = 100,
    defaultOffset: Int = 0,
): PaginationOptions {
    return PaginationOptions(
        limit = parameters["limit"]?.toInt() ?: defaultLimit,
        offset = parameters["offset"]?.toInt() ?: defaultOffset
    )
}

public data class PaginatedListDTO<TItem>(
    val items: List<TItem>,
    val hasMoreItems: Boolean,
)

public fun <TItem, TItemDTO> PaginatedList<TItem>.toDTO(transform: (TItem) -> TItemDTO): PaginatedListDTO<TItemDTO> {
    return PaginatedListDTO(
        items = items.map(transform),
        hasMoreItems = hasMoreItems,
    )
}

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
    val jobType: String,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
    val stateChangedAt: Instant,
    val numFailures: Int,
)

public data class JobDetailsDTO(
    val queueId: String,
    val jobId: String,
    val jobType: String,
    val state: String,
    val dataJson: String,
    val metadataJson: String,
    val concurrencyKey: String?,
    val createdAt: Instant,
    val firstScheduledFor: Instant,
    val nextScheduledFor: Instant?,
    val stateChangedAt: Instant,
    val watchdogTimestamp: Instant?,
    val watchdogOwner: String?,
    val numFailures: Int,
    val expiresAt: Instant?,
)

public data class JobFailureDTO(
    val failureId: String,
    val jobId: String,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTrace: String,
)