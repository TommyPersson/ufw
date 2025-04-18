package io.tpersson.ufw.cluster.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import jakarta.inject.Inject
import jakarta.inject.Singleton


@Singleton
public class ClusterComponent @Inject constructor() : Component<ClusterComponent> {

    public companion object : ComponentKey<ClusterComponent> {
    }
}

public val ComponentRegistry.cluster: ClusterComponent get() = get(ClusterComponent)