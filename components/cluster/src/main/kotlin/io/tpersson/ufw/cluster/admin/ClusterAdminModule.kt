package io.tpersson.ufw.cluster.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.utils.getPaginationOptions
import jakarta.inject.Inject

public class ClusterAdminModule @Inject constructor(
    private val adminFacade: ClusterAdminFacade,
) : AdminModule {
    override val moduleId: String = "cluster"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/cluster/instances") {
                val paginationOptions = call.getPaginationOptions()
                call.respond(adminFacade.getInstances(paginationOptions))
            }
        }
    }
}