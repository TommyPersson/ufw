package io.tpersson.ufw.admin.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule

public class CoreAdminModule(
) : AdminModule {
    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/ping") {
                call.respondText("pong", ContentType.Text.Plain)
            }

            get("/admin/api/core/application-metadata") {
                call.respond(
                    ApplicationMetadata(
                        name = "Example",
                        version = "2",
                    )
                )
            }
        }
    }
}