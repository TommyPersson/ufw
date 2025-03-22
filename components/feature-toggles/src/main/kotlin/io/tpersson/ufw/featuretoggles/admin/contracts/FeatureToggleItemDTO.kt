package io.tpersson.ufw.featuretoggles.admin.contracts

import java.time.Instant

public data class FeatureToggleItemDTO(
    val id: String,
    val isEnabled: Boolean,
    val stateChangedAt: Instant,
)