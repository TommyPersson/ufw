package io.tpersson.ufw.mediator.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import jakarta.inject.Inject

public class AdminRequestsAdminModule @Inject constructor(
    private val adminFacade: AdminRequestsAdminFacade
) : AdminModule {
    override val moduleId: String = "admin-requests"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/admin-requests/commands") {
                call.respond(adminFacade.getRequests(RequestType.COMMAND))
            }
            get("/admin/api/admin-requests/queries") {
                call.respond(adminFacade.getRequests(RequestType.QUERY))
            }
        }
    }
}

