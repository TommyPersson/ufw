package io.tpersson.ufw.jobqueue.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import jakarta.inject.Inject

public class JobQueueAdminModule @Inject constructor(
) : AdminModule {
    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/job-queue/hello") {
                call.respondText("Hello, Jobs!")
            }
        }
    }
}