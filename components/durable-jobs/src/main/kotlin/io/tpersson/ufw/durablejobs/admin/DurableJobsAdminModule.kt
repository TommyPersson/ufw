package io.tpersson.ufw.durablejobs.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.contracts.toDTO
import io.tpersson.ufw.admin.utils.getPaginationOptions
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacade
import io.tpersson.ufw.databasequeue.convertQueueId
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import io.tpersson.ufw.durablejobs.admin.contracts.*
import io.tpersson.ufw.durablejobs.internal.*
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

public class DurableJobsAdminModule @Inject constructor(
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    private val databaseQueueAdminFacade: DatabaseQueueAdminFacade,
) : AdminModule {

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
                            val stats = databaseQueueAdminFacade.getQueueStatistics(queueId.toWorkItemQueueId())
                            val status = databaseQueueAdminFacade.getQueueStatus(queueId.toWorkItemQueueId())

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

                val stats = databaseQueueAdminFacade.getQueueStatistics(queueId.toWorkItemQueueId())
                val status = databaseQueueAdminFacade.getQueueStatus(queueId.toWorkItemQueueId())

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

                databaseQueueAdminFacade.rescheduleAllFailedWorkItems(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/actions/delete-all-failed-jobs") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.deleteAllFailedWorkItems(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/actions/pause") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.pauseQueue(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/actions/unpause") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.unpauseQueue(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            get("/admin/api/durable-jobs/queues/{queueId}/jobs") {
                val queueId = call.parameters.queueId!!
                val jobState = call.parameters.state!!

                val paginationOptions = call.getPaginationOptions()

                val paginatedItems = databaseQueueAdminFacade.getWorkItems(
                    queueId = queueId.toWorkItemQueueId(),
                    state = jobState,
                    paginationOptions = paginationOptions
                )

                val jobList = paginatedItems.toDTO { it.toItemDTO() }

                call.respond(jobList)
            }

            get("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/details") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                val job = databaseQueueAdminFacade.getWorkItem(
                    queueId = queueId.toWorkItemQueueId(),
                    itemId = jobId.toWorkItemId()
                )?.toDetailsDTO()

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

                val failures = databaseQueueAdminFacade.getWorkItemFailures(
                    queueId = queueId.toWorkItemQueueId(),
                    itemId = jobId.toWorkItemId(),
                    paginationOptions = paginationOptions
                ).toDTO { it.toDTO(jobId) }

                call.respond(failures)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/actions/delete") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                databaseQueueAdminFacade.deleteFailedWorkItem(queueId.toWorkItemQueueId(), jobId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/actions/reschedule-now") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                databaseQueueAdminFacade.rescheduleFailedWorkItem(queueId.toWorkItemQueueId(), jobId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-jobs/queues/{queueId}/jobs/{jobId}/actions/cancel") {
                val queueId = call.parameters.queueId!!
                val jobId = call.parameters.jobId!!

                databaseQueueAdminFacade.cancelWorkItem(queueId.toWorkItemQueueId(), jobId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    private fun WorkItemDbEntity.toItemDTO() = JobItemDTO(
        jobId = this.itemId,
        jobType = this.type,
        createdAt = this.createdAt,
        firstScheduledFor = this.firstScheduledFor,
        nextScheduledFor = this.nextScheduledFor,
        stateChangedAt = this.stateChangedAt,
        numFailures = this.numFailures,
    )

    private fun WorkItemFailureDbEntity.toDTO(
        jobId: DurableJobId
    ) = JobFailureDTO(
        failureId = this.id,
        jobId = jobId.value,
        timestamp = this.timestamp,
        errorType = this.errorType,
        errorMessage = this.errorMessage,
        errorStackTrace = this.errorStackTrace,
    )

    private fun WorkItemDbEntity.toDetailsDTO() = JobDetailsDTO(
        queueId = DurableJobsDatabaseQueueAdapterSettings.convertQueueId(WorkItemQueueId(queueId)),
        jobId = this.itemId,
        jobType = this.type,
        state = WorkItemState.fromDbOrdinal(this.state).name,
        dataJson = this.dataJson,
        metadataJson = this.metadataJson,
        concurrencyKey = this.concurrencyKey,
        createdAt = this.createdAt,
        firstScheduledFor = this.firstScheduledFor,
        nextScheduledFor = this.nextScheduledFor,
        stateChangedAt = this.stateChangedAt,
        watchdogTimestamp = this.watchdogTimestamp,
        watchdogOwner = this.watchdogOwner,
        numFailures = this.numFailures,
        expiresAt = this.expiresAt,
    )
}

private val Parameters.queueId: DurableJobQueueId? get() = this["queueId"]?.let { DurableJobQueueId.fromString(it) }
private val Parameters.jobId: DurableJobId? get() = this["jobId"]?.let { DurableJobId.fromString(it) }
private val Parameters.state: WorkItemState? get() = this["state"]?.let { WorkItemState.fromString(it) }

