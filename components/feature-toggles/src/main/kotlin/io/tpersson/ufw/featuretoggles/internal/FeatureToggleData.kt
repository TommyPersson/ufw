package io.tpersson.ufw.featuretoggles.internal

import java.time.Instant

public data class FeatureToggleData(
    val id: String,
    val isEnabled: Boolean,
    val stateChangedAt: Instant,
)