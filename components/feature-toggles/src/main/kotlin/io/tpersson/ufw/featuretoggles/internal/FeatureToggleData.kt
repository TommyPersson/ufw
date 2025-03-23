package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition
import java.time.Instant

public data class FeatureToggleData(
    val definition: FeatureToggleDefinition,
    val isEnabled: Boolean,
    val stateChangedAt: Instant,
)