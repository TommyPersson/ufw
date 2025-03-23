package io.tpersson.ufw.featuretoggles

public interface FeatureToggleHandle {
    public val definition: FeatureToggleDefinition

    // TODO allow caching
    public suspend fun get(): FeatureToggle

    // TODO allow caching
    public suspend fun isEnabled(): Boolean

    public suspend fun enable()

    public suspend fun disable()
}

