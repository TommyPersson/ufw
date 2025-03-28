package io.tpersson.ufw.featuretoggles.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import java.time.Instant

public data class FeatureToggleItemDTO(
    val id: String,
    val title: String,
    val description: String,
    val stateChangedAt: Instant,
    val createdAt: Instant,
    val isEnabled: Boolean,
    val applicationModule: ApplicationModuleDTO,
)