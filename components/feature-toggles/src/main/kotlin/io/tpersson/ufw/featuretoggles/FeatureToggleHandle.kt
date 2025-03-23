package io.tpersson.ufw.featuretoggles

public interface FeatureToggleHandle {
    public val definition: FeatureToggleDefinition

    public suspend fun isEnabled(): Boolean

    public suspend fun enable()

    public suspend fun disable()
}

