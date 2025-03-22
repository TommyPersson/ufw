package io.tpersson.ufw.featuretoggles.internal

import java.time.Instant

public data class FeatureToggle(
    val id: String,
    val isEnabled: Boolean,
    val stateChangedAt: Instant,
)