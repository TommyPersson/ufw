package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition
import io.tpersson.ufw.featuretoggles.FeatureToggles

public interface FeatureTogglesInternal : FeatureToggles {
    public val knownFeatureToggles: Map<String, FeatureToggleDefinition>
}