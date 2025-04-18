package io.tpersson.ufw.cluster.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.cluster.admin.ClusterAdminFacadeImpl
import io.tpersson.ufw.cluster.admin.ClusterAdminModule
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.keyvaluestore.component.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.component.keyValueStore

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

    override fun build(components: ComponentRegistryInternal): ClusterComponent {
        val durableCachesAdminModule = ClusterAdminModule(
            adminFacade = ClusterAdminFacadeImpl(
                keyValueStore = components.keyValueStore.keyValueStore,
                appInfoProvider = components.core.appInfoProvider
            )
        )

        components.admin.register(durableCachesAdminModule)

        return ClusterComponent()
    }
}