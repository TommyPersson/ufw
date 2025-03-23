package io.tpersson.ufw.featuretoggles.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.contracts.toDTO
import io.tpersson.ufw.admin.utils.getPaginationOptions
import io.tpersson.ufw.featuretoggles.admin.contracts.FeatureToggleItemDTO
import io.tpersson.ufw.featuretoggles.FeatureToggle
import jakarta.inject.Inject

public class FeatureTogglesAdminModule @Inject constructor(
    private val featureTogglesAdminFacade: FeatureTogglesAdminFacade,
) : AdminModule {

    public override val moduleId: String = "feature-toggles"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/feature-toggles/feature-toggles") {
                val paginationOptions = call.getPaginationOptions()

                val featureToggles = featureTogglesAdminFacade.listAll(paginationOptions)
                val dtos = featureToggles.toDTO { it.toItemDTO() }

                call.respond(dtos)
            }

            post("/admin/api/feature-toggles/feature-toggles/{id}/actions/enable") {
                val featureToggleId = call.parameters.featureToggleId!!

                featureTogglesAdminFacade.enable(featureToggleId)

                call.respond(HttpStatusCode.NoContent)
            }

            post("/admin/api/feature-toggles/feature-toggles/{id}/actions/disable") {
                val featureToggleId = call.parameters.featureToggleId!!

                featureTogglesAdminFacade.disable(featureToggleId)

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    private fun FeatureToggle.toItemDTO(): FeatureToggleItemDTO {
        return FeatureToggleItemDTO(
            id = id,
            title = title,
            description = description,
            stateChangedAt = stateChangedAt,
            createdAt = createdAt,
            isEnabled = isEnabled,
        )
    }
}

private val Parameters.featureToggleId: String? get() = this["id"]

