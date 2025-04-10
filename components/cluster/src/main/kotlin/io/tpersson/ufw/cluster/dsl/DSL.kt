package io.tpersson.ufw.cluster.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.cluster.ClusterComponent
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore

@UfwDslMarker
public fun UFWBuilder.RootBuilder.cluster(builder: ClusterComponentBuilder.() -> Unit = {}) {
    components["Cluster"] = ClusterComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class ClusterComponentBuilder(public val components: UFWRegistry) {
    public fun build(): ClusterComponent {
        return ClusterComponent.create(
            coreComponent = components.core,
            keyValueStoreComponent = components.keyValueStore,
            adminComponent = components.admin,
        )
    }
}

public val UFWRegistry.cluster: ClusterComponent get() = _components["Cluster"] as ClusterComponent