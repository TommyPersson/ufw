package io.tpersson.ufw.cluster.builder

import io.tpersson.ufw.admin.builder.admin
import io.tpersson.ufw.admin.builder.installAdmin
import io.tpersson.ufw.cluster.ClusterComponent
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.keyvaluestore.builder.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.builder.keyValueStore

@UfwDslMarker
public fun UFWBuilder.Root.installCluster(configure: ClusterComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installKeyValueStore()
    installAdmin()

    val ctx = contexts.getOrPut(ClusterComponent) { ClusterComponentBuilderContext() }
        .also(configure)

    builders.add(ClusterComponentBuilder(ctx))
}

public class ClusterComponentBuilderContext : ComponentBuilderContext<ClusterComponent>

public class ClusterComponentBuilder(
    private val context: ClusterComponentBuilderContext
) : ComponentBuilder<ClusterComponent> {

    override fun build(components: ComponentRegistry): ClusterComponent {
        return ClusterComponent.create(
            coreComponent = components.core,
            keyValueStoreComponent = components.keyValueStore,
            adminComponent = components.admin,
        )
    }
}

public val ComponentRegistry.cluster: ClusterComponent get() = get(ClusterComponent)