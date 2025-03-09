package io.tpersson.ufw.jobqueue.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.jobqueue.internal.JobQueueInternal
import io.tpersson.ufw.jobqueue.JobQueueId
import io.tpersson.ufw.jobqueue.internal.DurableJobDefinition
import io.tpersson.ufw.jobqueue.internal.DurableJobHandlersProvider
import io.tpersson.ufw.jobqueue.internal.jobDefinition
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

public class JobQueueAdminModule @Inject constructor(
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    private val jobQueue: JobQueueInternal
) : AdminModule {

    public override val moduleId: String = "job-queue"

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
                val queueId = JobQueueId.fromString(call.parameters["queueId"]!!)

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
        }
    }
}

public data class QueueListItemDTO(
    val queueId: JobQueueId,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
)

public data class QueueDetailsDTO(
    val queueId: JobQueueId,
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