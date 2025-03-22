package io.tpersson.ufw.featuretoggles

public interface FeatureToggleHandle {
    public val featureToggleId: String

    public suspend fun isEnabled(default: Boolean = false): Boolean

    public suspend fun enable()

    public suspend fun disable()
}

