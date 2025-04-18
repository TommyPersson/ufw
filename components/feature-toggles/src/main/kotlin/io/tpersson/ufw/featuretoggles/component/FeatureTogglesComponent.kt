package io.tpersson.ufw.featuretoggles.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.featuretoggles.FeatureToggles
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class FeatureTogglesComponent @Inject constructor(
    public val featureToggles: FeatureToggles
) : Component<FeatureTogglesComponent> {

    public companion object : ComponentKey<FeatureTogglesComponent> {
    }
}

public val ComponentRegistry.featureToggles: FeatureTogglesComponent get() = get(FeatureTogglesComponent)