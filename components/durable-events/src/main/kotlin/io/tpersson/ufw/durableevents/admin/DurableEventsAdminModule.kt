package io.tpersson.ufw.durableevents.admin

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
import io.tpersson.ufw.core.utils.nullIfBlank
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.WorkItemState
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacade
import io.tpersson.ufw.databasequeue.convertQueueId
import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import io.tpersson.ufw.databasequeue.internal.WorkItemFailureDbEntity
import io.tpersson.ufw.durableevents.admin.contracts.*
import io.tpersson.ufw.durableevents.common.DurableEventId
import io.tpersson.ufw.durableevents.common.DurableEventQueueId
import io.tpersson.ufw.durableevents.handler.internal.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass

@Singleton
public class DurableEventsAdminModule @Inject constructor(
    private val durableEventHandlersProvider: DurableEventHandlersProvider,
    private val databaseQueueAdminFacade: DatabaseQueueAdminFacade,
) : AdminModule {

    public override val moduleId: String = "durable-events"

    private val eventHandlersByQueueId: Map<DurableEventQueueId, List<DurableEventHandlerMethod<*>>>
            by Memoized({ durableEventHandlersProvider.get() }) { handlers ->
                handlers.associate { it.queueId to it.findHandlerMethods() }
            }

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/durable-events/queues") {
                coroutineScope {
                    val listItems = eventHandlersByQueueId.map { (queueId, handlers) ->
                        async {
                            val stats = databaseQueueAdminFacade.getQueueStatistics(queueId.toWorkItemQueueId())
                            val status = databaseQueueAdminFacade.getQueueStatus(queueId.toWorkItemQueueId())
                            val module = handlers[0].handler::class.findModuleMolecule()

                            QueueListItemDTO(
                                queueId = queueId,
                                numScheduled = stats.numScheduled,
                                numPending = stats.numPending,
                                numInProgress = stats.numInProgress,
                                numFailed = stats.numFailed,
                                status = EventQueueStatusDTO(
                                    state = status.state,
                                    stateChangedAt = status.stateChangedAt,
                                ),
                                applicationModule = module.toApplicationModuleDTO()

                            )
                        }
                    }.awaitAll()

                    call.respond(listItems)
                }
            }

            get("/admin/api/durable-events/queues/{queueId}/details") {
                val queueId = call.parameters.queueId!!

                val handlers = eventHandlersByQueueId[queueId]
                    ?: HttpStatusCode.NotFound.raise()

                val stats = databaseQueueAdminFacade.getQueueStatistics(queueId.toWorkItemQueueId())
                val status = databaseQueueAdminFacade.getQueueStatus(queueId.toWorkItemQueueId())
                val module = handlers[0].handler::class.findModuleMolecule()

                val details = QueueDetailsDTO(
                    queueId = queueId,
                    numScheduled = stats.numScheduled,
                    numPending = stats.numPending,
                    numInProgress = stats.numInProgress,
                    numFailed = stats.numFailed,
                    status = status.let {
                        EventQueueStatusDTO(
                            state = it.state,
                            stateChangedAt = it.stateChangedAt,
                        )
                    },
                    eventTypes = handlers.map {
                        QueueDetailsDTO.EventType(
                            typeName = it.eventType,
                            className = it.eventClass.simpleName!!,
                            description = it.eventDescription.nullIfBlank()
                        )
                    },
                    applicationModule = module.toApplicationModuleDTO()
                )

                call.respond(details)
            }

            post("/admin/api/durable-events/queues/{queueId}/actions/reschedule-all-failed-events") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.rescheduleAllFailedWorkItems(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-events/queues/{queueId}/actions/delete-all-failed-events") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.deleteAllFailedWorkItems(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-events/queues/{queueId}/actions/pause") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.pauseQueue(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-events/queues/{queueId}/actions/unpause") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.unpauseQueue(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            get("/admin/api/durable-events/queues/{queueId}/events") {
                val queueId = call.parameters.queueId!!
                val state = call.parameters.state!!

                val paginationOptions = call.getPaginationOptions()

                val paginatedItems = databaseQueueAdminFacade.getWorkItems(
                    queueId = queueId.toWorkItemQueueId(),
                    state = state,
                    paginationOptions = paginationOptions
                )

                val jobList = paginatedItems.toDTO { it.toItemDTO() }

                call.respond(jobList)
            }

            get("/admin/api/durable-events/queues/{queueId}/events/{eventId}/details") {
                val queueId = call.parameters.queueId!!
                val eventId = call.parameters.eventId!!

                val event = databaseQueueAdminFacade.getWorkItem(
                    queueId = queueId.toWorkItemQueueId(),
                    itemId = eventId.toWorkItemId()
                )?.let { item ->
                    val handler = eventHandlersByQueueId[queueId]?.first { it.eventType == item.type }
                        ?: HttpStatusCode.BadRequest.raise()

                    item.toDetailsDTO(
                        eventClass = handler.eventClass,
                        description = handler.eventDescription,
                    )
                }

                if (event == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(event)
            }

            get("/admin/api/durable-events/queues/{queueId}/events/{eventId}/failures") {
                val queueId = call.parameters.queueId!!
                val eventId = call.parameters.eventId!!

                val paginationOptions = call.getPaginationOptions(defaultLimit = 5)

                val failures = databaseQueueAdminFacade.getWorkItemFailures(
                    queueId = queueId.toWorkItemQueueId(),
                    itemId = eventId.toWorkItemId(),
                    paginationOptions = paginationOptions
                ).toDTO { it.toDTO(eventId) }

                call.respond(failures)
            }

            post("/admin/api/durable-events/queues/{queueId}/events/{eventId}/actions/delete") {
                val queueId = call.parameters.queueId!!
                val eventId = call.parameters.eventId!!

                databaseQueueAdminFacade.deleteFailedWorkItem(queueId.toWorkItemQueueId(), eventId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-events/queues/{queueId}/events/{eventId}/actions/reschedule-now") {
                val queueId = call.parameters.queueId!!
                val eventId = call.parameters.eventId!!

                databaseQueueAdminFacade.rescheduleFailedWorkItem(queueId.toWorkItemQueueId(), eventId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-events/queues/{queueId}/events/{eventId}/actions/cancel") {
                val queueId = call.parameters.queueId!!
                val eventId = call.parameters.eventId!!

                databaseQueueAdminFacade.cancelWorkItem(queueId.toWorkItemQueueId(), eventId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    private fun WorkItemDbEntity.toItemDTO() = EventItemDTO(
        eventId = this.itemId,
        eventType = this.type,
        createdAt = this.createdAt,
        firstScheduledFor = this.firstScheduledFor,
        nextScheduledFor = this.nextScheduledFor,
        stateChangedAt = this.stateChangedAt,
        numFailures = this.numFailures,
    )

    private fun WorkItemFailureDbEntity.toDTO(
        eventId: DurableEventId
    ) = EventFailureDTO(
        failureId = this.id,
        eventId = eventId.value,
        timestamp = this.timestamp,
        errorType = this.errorType,
        errorMessage = this.errorMessage,
        errorStackTrace = this.errorStackTrace,
    )

    private fun WorkItemDbEntity.toDetailsDTO(eventClass: KClass<*>, description: String) = EventDetailsDTO(
        queueId = DurableEventsDatabaseQueueAdapterSettings.convertQueueId(WorkItemQueueId(queueId)),
        eventId = this.itemId,
        eventType = this.type,
        eventTypeClass = eventClass.simpleName ?: "<unknown>",
        eventTypeDescription = description.nullIfBlank(),
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

private val Parameters.queueId: DurableEventQueueId? get() = this["queueId"]?.let { DurableEventQueueId.fromString(it) }
private val Parameters.eventId: DurableEventId? get() = this["eventId"]?.let { DurableEventId.fromString(it) }
private val Parameters.state: WorkItemState? get() = this["state"]?.let { WorkItemState.fromString(it) }

