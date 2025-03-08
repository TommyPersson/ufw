package io.tpersson.ufw.jobqueue.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.jobqueue.v2.internal.DurableJobHandlersProvider
import io.tpersson.ufw.jobqueue.v2.internal.jobDefinition
import jakarta.inject.Inject

public class JobQueueAdminModule @Inject constructor(
    private val durableJobHandlersProvider: DurableJobHandlersProvider
) : AdminModule {

    public override val moduleId: String = "job-queue"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/job-queue/hello") {
                call.respondText("Hello, Jobs!")
            }

            get("/admin/api/job-queue/queues") {
                val queueIds = durableJobHandlersProvider.get().map { it.jobDefinition.queueId }
                val listItems = queueIds.map {
                    QueueListItemDTO(
                        queueId = it
                    )
                }
                call.respond(listItems)
            }
        }
    }
}

public data class QueueListItemDTO(
    val queueId: String,
)