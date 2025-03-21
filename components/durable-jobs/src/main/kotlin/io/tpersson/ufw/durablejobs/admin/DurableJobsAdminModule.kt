package io.tpersson.ufw.durablejobs.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.contracts.toDTO
import io.tpersson.ufw.admin.utils.getPaginationOptions
import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import io.tpersson.ufw.durablejobs.admin.contracts.*
import io.tpersson.ufw.durablejobs.internal.DurableJobDefinition
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueInternal
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
                            val status = jobQueue.getQueueStatus(queueId)
                            QueueListItemDTO(
                                queueId = queueId,
                                numScheduled = stats.numScheduled,
                                numPending = stats.numPending,
                                numInProgress = stats.numInProgress,
                                numFailed = stats.numFailed,
                                status = JobQueueStatusDTO(
                                    state = status.state,
                                    stateChangedAt = status.stateChangedAt,
                                )
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
                val status = jobQueue.getQueueStatus(queueId)

                val details = QueueDetailsDTO(
                    queueId = queueId,
                    numScheduled = stats.numScheduled,
                    numPending = stats.numPending,
                    numInProgress = stats.numInProgress,
                    numFailed = stats.numFailed,
                    status = status.let {
                        JobQueueStatusDTO(
                            state = it.state,
                            stateChangedAt = it.stateChangedAt,
                        )
                    },
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

            post("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/actions/cancel") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                jobQueue.cancelJob(queueId, jobId, clock.instant())

                logger.info("Cancelled job: <$queueId/$jobId>")

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private val Parameters.queueId: DurableJobQueueId? get() = this["queueId"]?.let { DurableJobQueueId.fromString(it) }
private val Parameters.jobId: DurableJobId? get() = this["jobId"]?.let { DurableJobId.fromString(it) }
private val Parameters.state: WorkItemState? get() = this["state"]?.let { WorkItemState.fromString(it) }
