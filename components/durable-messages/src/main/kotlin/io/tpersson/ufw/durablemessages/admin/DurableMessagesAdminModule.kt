package io.tpersson.ufw.durablemessages.admin

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
import io.tpersson.ufw.durablemessages.admin.contracts.*
import io.tpersson.ufw.durablemessages.common.DurableMessageId
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId
import io.tpersson.ufw.durablemessages.handler.internal.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass

@Singleton
public class DurableMessagesAdminModule @Inject constructor(
    private val durableMessageHandlerRegistry: DurableMessageHandlerRegistry,
    private val databaseQueueAdminFacade: DatabaseQueueAdminFacade,
) : AdminModule {

    public override val moduleId: String = "durable-messages"

    private val messageHandlersByQueueId: Map<DurableMessageQueueId, List<DurableMessageHandlerMethod<*>>>
            by Memoized({ durableMessageHandlerRegistry.get() }) { handlers ->
                handlers.associate { it.queueId to it.findHandlerMethods() }
            }

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/durable-messages/queues") {
                coroutineScope {
                    val listItems = messageHandlersByQueueId.map { (queueId, handlers) ->
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
                                status = MessageQueueStatusDTO(
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

            get("/admin/api/durable-messages/queues/{queueId}/details") {
                val queueId = call.parameters.queueId!!

                val handlers = messageHandlersByQueueId[queueId]
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
                        MessageQueueStatusDTO(
                            state = it.state,
                            stateChangedAt = it.stateChangedAt,
                        )
                    },
                    messageTypes = handlers.map {
                        QueueDetailsDTO.MessageType(
                            typeName = it.messageType,
                            className = it.messageClass.simpleName!!,
                            description = it.messageDescription.nullIfBlank()
                        )
                    },
                    applicationModule = module.toApplicationModuleDTO()
                )

                call.respond(details)
            }

            post("/admin/api/durable-messages/queues/{queueId}/actions/reschedule-all-failed-messages") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.rescheduleAllFailedWorkItems(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-messages/queues/{queueId}/actions/delete-all-failed-messages") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.deleteAllFailedWorkItems(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-messages/queues/{queueId}/actions/pause") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.pauseQueue(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-messages/queues/{queueId}/actions/unpause") {
                val queueId = call.parameters.queueId!!

                databaseQueueAdminFacade.unpauseQueue(queueId.toWorkItemQueueId())

                call.respond(HttpStatusCode.NoContent)
            }

            get("/admin/api/durable-messages/queues/{queueId}/messages") {
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

            get("/admin/api/durable-messages/queues/{queueId}/messages/{messageId}/details") {
                val queueId = call.parameters.queueId!!
                val messageId = call.parameters.messageId!!

                val message = databaseQueueAdminFacade.getWorkItem(
                    queueId = queueId.toWorkItemQueueId(),
                    itemId = messageId.toWorkItemId()
                )?.let { item ->
                    val handler = messageHandlersByQueueId[queueId]?.first { it.messageType == item.type }
                        ?: HttpStatusCode.BadRequest.raise()

                    item.toDetailsDTO(
                        messageClass = handler.messageClass,
                        description = handler.messageDescription,
                    )
                }

                if (message == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(message)
            }

            get("/admin/api/durable-messages/queues/{queueId}/messages/{messageId}/failures") {
                val queueId = call.parameters.queueId!!
                val messageId = call.parameters.messageId!!

                val paginationOptions = call.getPaginationOptions(defaultLimit = 5)

                val failures = databaseQueueAdminFacade.getWorkItemFailures(
                    queueId = queueId.toWorkItemQueueId(),
                    itemId = messageId.toWorkItemId(),
                    paginationOptions = paginationOptions
                ).toDTO { it.toDTO(messageId) }

                call.respond(failures)
            }

            post("/admin/api/durable-messages/queues/{queueId}/messages/{messageId}/actions/delete") {
                val queueId = call.parameters.queueId!!
                val messageId = call.parameters.messageId!!

                databaseQueueAdminFacade.deleteFailedWorkItem(queueId.toWorkItemQueueId(), messageId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-messages/queues/{queueId}/messages/{messageId}/actions/reschedule-now") {
                val queueId = call.parameters.queueId!!
                val messageId = call.parameters.messageId!!

                databaseQueueAdminFacade.rescheduleFailedWorkItem(queueId.toWorkItemQueueId(), messageId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/durable-messages/queues/{queueId}/messages/{messageId}/actions/cancel") {
                val queueId = call.parameters.queueId!!
                val messageId = call.parameters.messageId!!

                databaseQueueAdminFacade.cancelWorkItem(queueId.toWorkItemQueueId(), messageId.toWorkItemId())

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    private fun WorkItemDbEntity.toItemDTO() = MessageItemDTO(
        messageId = this.itemId,
        messageType = this.type,
        createdAt = this.createdAt,
        firstScheduledFor = this.firstScheduledFor,
        nextScheduledFor = this.nextScheduledFor,
        stateChangedAt = this.stateChangedAt,
        numFailures = this.numFailures,
    )

    private fun WorkItemFailureDbEntity.toDTO(
        messageId: DurableMessageId
    ) = MessageFailureDTO(
        failureId = this.id,
        messageId = messageId.value,
        timestamp = this.timestamp,
        errorType = this.errorType,
        errorMessage = this.errorMessage,
        errorStackTrace = this.errorStackTrace,
    )

    private fun WorkItemDbEntity.toDetailsDTO(messageClass: KClass<*>, description: String) = MessageDetailsDTO(
        queueId = DurableMessagesDatabaseQueueAdapterSettings.convertQueueId(WorkItemQueueId(queueId)),
        messageId = this.itemId,
        messageType = this.type,
        messageTypeClass = messageClass.simpleName ?: "<unknown>",
        messageTypeDescription = description.nullIfBlank(),
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

private val Parameters.queueId: DurableMessageQueueId? get() = this["queueId"]?.let { DurableMessageQueueId.fromString(it) }
private val Parameters.messageId: DurableMessageId? get() = this["messageId"]?.let { DurableMessageId.fromString(it) }
private val Parameters.state: WorkItemState? get() = this["state"]?.let { WorkItemState.fromString(it) }

