package io.tpersson.ufw.featuretoggles

public interface FeatureToggles {
    public fun get(definition: FeatureToggleDefinition): FeatureToggleHandle
}