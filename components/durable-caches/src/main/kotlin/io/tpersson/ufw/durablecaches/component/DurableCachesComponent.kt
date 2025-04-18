package io.tpersson.ufw.durablecaches.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.durablecaches.DurableCaches
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class DurableCachesComponent @Inject constructor(
    public val durableCaches: DurableCaches
) : Component<DurableCachesComponent> {

    public companion object : ComponentKey<DurableCachesComponent> {
    }
}

public val ComponentRegistry.durableCaches: DurableCachesComponent get() = get(DurableCachesComponent)

