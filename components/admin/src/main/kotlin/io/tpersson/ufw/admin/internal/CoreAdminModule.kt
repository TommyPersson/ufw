package io.tpersson.ufw.admin.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.contracts.ApplicationMetadataDTO
import io.tpersson.ufw.core.AppInfoProvider

public class CoreAdminModule(
    private val modulesProvider: AdminModulesRegistry,
    private val appInfoProvider: AppInfoProvider,
) : AdminModule {

    override val moduleId: String = "core"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/ping") {
                call.respondText("pong", ContentType.Text.Plain)
            }

            get("/admin/api/core/application-metadata") {
                val appInfo = appInfoProvider.get()
                call.respond(
                    ApplicationMetadataDTO(
                        name = appInfo.name,
                        version = appInfo.version,
                        environment = appInfo.environment,
                        availableModuleIds = modulesProvider.get().map { it.moduleId }
                    )
                )
            }
        }
    }
}