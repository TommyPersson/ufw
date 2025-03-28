package io.tpersson.ufw.featuretoggles

import java.time.Instant

public data class FeatureToggle(
    val definition: FeatureToggleDefinition,
    val id: String,
    val title: String,
    val description: String,
    val stateChangedAt: Instant,
    val createdAt: Instant,
    val isEnabled: Boolean,
)