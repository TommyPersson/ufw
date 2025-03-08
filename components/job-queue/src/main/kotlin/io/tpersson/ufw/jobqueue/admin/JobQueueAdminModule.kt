package io.tpersson.ufw.jobqueue.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.jobqueue.internal.JobQueueInternal
import io.tpersson.ufw.jobqueue.v2.internal.DurableJobHandlersProvider
import io.tpersson.ufw.jobqueue.v2.internal.jobDefinition
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

public class JobQueueAdminModule @Inject constructor(
    private val durableJobHandlersProvider: DurableJobHandlersProvider,
    private val jobQueue: JobQueueInternal
) : AdminModule {

    public override val moduleId: String = "job-queue"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/job-queue/hello") {
                call.respondText("Hello, Jobs!")
            }

            get("/admin/api/job-queue/queues") {
                coroutineScope {
                    val queueIds = durableJobHandlersProvider.get().map { it.jobDefinition.queueId }
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
        }
    }
}

public data class QueueListItemDTO(
    val queueId: String,
    val numScheduled: Int,
    val numPending: Int,
    val numInProgress: Int,
    val numFailed: Int,
)