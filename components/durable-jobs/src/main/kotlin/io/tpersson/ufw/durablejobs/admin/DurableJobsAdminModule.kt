package io.tpersson.ufw.durablejobs.admin

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.contracts.toApplicationModuleDTO
import io.tpersson.ufw.admin.contracts.toDTO
import io.tpersson.ufw.admin.raise
import io.tpersson.ufw.admin.utils.getPaginationOptions
import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.core.utils.findModuleMolecule
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacade
import io.tpersson.ufw.databasequeue.convertQueueId
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.durablejobs.DurableJobId
import io.tpersson.ufw.durablejobs.DurableJobQueueId
import io.tpersson.ufw.durablejobs.periodic.PeriodicJob
import io.tpersson.ufw.durablejobs.admin.contracts.*
import io.tpersson.ufw.durablejobs.internal.*
import io.tpersson.ufw.durablejobs.periodic.internal.PeriodicJobManager
import io.tpersson.ufw.durablejobs.periodic.internal.key
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.*
import kotlin.reflect.full.findAnnotation

public class DurableJobsAdminModule @Inject constructor(
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    private val periodicJobManager: PeriodicJobManager,
    private val databaseQueueAdminFacade: DatabaseQueueAdminFacade,
) : AdminModule {

    public override val moduleId: String = "durable-jobs"

    private val jobHandlerDefinitions: List<DurableJobDefinition<*>>
            by Memoized({ durableJobHandlersProvider.get() }) { handlers ->
                handlers.map { it.jobDefinition }.sortedBy { it.queueId }
            }

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/durable-jobs/queues") {
                coroutineScope {
                    val listItems = jobHandlerDefinitions.map { handler ->
                        async { // TODO what about queues with multiple handlers? should use distinct
                            val stats = databaseQueueAdminFacade.getQueueStatistics(handler.queueId.toWorkItemQueueId())
                            val status = databaseQueueAdminFacade.getQueueStatus(handler.queueId.toWorkItemQueueId())
                            val module = handler.jobClass.findModuleMolecule()

                            QueueListItemDTO(
                                queueId = handler.queueId,
                                numScheduled = stats.numScheduled,
                                numPending = stats.numPending,
                                numInProgress = stats.numInProgress,
                                numFailed = stats.numFailed,
                                status = JobQueueStatusDTO(
                                    state = status.state,
                                    stateChangedAt = status.stateChangedAt,
                                ),
                                hasOnlyPeriodicJobTypes = handler.jobClass.findAnnotation<PeriodicJob>() != null, // TODO should check all job types
                                applicationModule = module.toApplicationModuleDTO()

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
                val module = handlers.first().jobClass.findModuleMolecule()

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
                            typeName = it.type,
                            className = it.jobClass.simpleName!!,
                            description = it.description,
                            periodic = it.jobClass.findAnnotation<PeriodicJob>() != null,
                            periodicCron = it.jobClass.findAnnotation<PeriodicJob>()?.cronExpression,
                            periodicCronExplanation = it.jobClass.findAnnotation<PeriodicJob>()?.cronExpression?.let { cronExpression ->
                                CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX).let { cronDef ->
                                    CronParser(cronDef).parse(cronExpression).let { cron ->
                                        CronDescriptor.instance(Locale.US).describe(cron)
                                    }
                                }
                            }

                        )
                    },
                    applicationModule = module.toApplicationModuleDTO()
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
                )?.let { item ->
                    val jobDefinition = jobHandlerDefinitions.first { it.type == item.type }
                    item.toDetailsDTO(jobDefinition)
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

            get("/admin/api/durable-jobs/periodic-jobs") {
                val stateByKey = periodicJobManager.getState().associateBy { it.key }

                val periodicJobs = periodicJobManager.periodicJobSpecs.map { spec ->
                    val state = stateByKey[spec.key]
                    val jobDefinition = spec.handler.jobDefinition
                    val jobQueueId = jobDefinition.queueId
                    val workQueueId = jobQueueId.toWorkItemQueueId()

                    PeriodicJobDTO(
                        type = jobDefinition.type,
                        description = jobDefinition.description,
                        cronExpression = spec.cronExpression,
                        cronDescription = CronDescriptor.instance(Locale.US).describe(spec.cronInstance),
                        lastSchedulingAttempt = state?.lastSchedulingAttempt,
                        nextSchedulingAttempt = state?.nextSchedulingAttempt,
                        queueId = jobQueueId,
                        queueState = databaseQueueAdminFacade.getQueueStatus(queueId = workQueueId).state,
                        queueNumFailures = databaseQueueAdminFacade.getQueueStatistics(workQueueId).numFailed,
                        lastExecutionState = state?.lastExecutionState?.let { WorkItemState.fromDbOrdinal(it) },
                        lastExecutionStateChangeTimestamp = state?.lastExecutionStateChangeTimestamp,
                        applicationModule = jobDefinition.jobClass.findModuleMolecule().toApplicationModuleDTO()
                    )
                }

                call.respond(periodicJobs)
            }

            post("/admin/api/durable-jobs/periodic-jobs/{queueId}/{jobType}/actions/schedule-now") {
                val queueId = call.parameters.queueId
                val jobType = call.parameters["jobType"]

                val periodicJob = periodicJobManager.periodicJobSpecs.firstOrNull {
                    it.handler.jobDefinition.type == jobType && it.handler.jobDefinition.queueId == queueId
                } ?: HttpStatusCode.NotFound.raise()

                val jobId = periodicJobManager.scheduleJobNow(periodicJob)

                call.respond(
                    SchedulePeriodicJobNowResponseDTO(jobId = jobId)
                )
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

    private fun WorkItemDbEntity.toDetailsDTO(jobDefinition: DurableJobDefinition<*>) = JobDetailsDTO(
        queueId = DurableJobsDatabaseQueueAdapterSettings.convertQueueId(WorkItemQueueId(queueId)),
        jobId = this.itemId,
        jobType = this.type,
        jobTypeClass = jobDefinition.jobClass.simpleName ?: "<unknown>",
        jobTypeDescription = jobDefinition.description,
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

