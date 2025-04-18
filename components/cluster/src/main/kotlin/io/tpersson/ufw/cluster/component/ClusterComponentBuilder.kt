package io.tpersson.ufw.cluster.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.cluster.admin.ClusterAdminFacadeImpl
import io.tpersson.ufw.cluster.admin.ClusterAdminModule
import io.tpersson.ufw.cluster.internal.ClusterInstancesServiceImpl
import io.tpersson.ufw.cluster.internal.ClusterInstanceHeartbeatWorker
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.keyvaluestore.component.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.component.keyValueStore
import io.tpersson.ufw.managed.component.installManaged
import io.tpersson.ufw.managed.component.managed

@UfwDslMarker
public fun UFWBuilder.Root.installCluster(configure: ClusterComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installManaged()
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

        val clusterInstancesService = ClusterInstancesServiceImpl(
            keyValueStore = components.keyValueStore.keyValueStore,
            appInfoProvider = components.core.appInfoProvider,
            configProvider = components.core.configProvider,
            clock = components.core.clock,
        )

        val durableCachesAdminModule = ClusterAdminModule(
            adminFacade = ClusterAdminFacadeImpl(
                clusterInstancesService = clusterInstancesService
            )
        )

        val heartbeatWorker = ClusterInstanceHeartbeatWorker(
            clusterInstancesService = clusterInstancesService,
            configProvider = components.core.configProvider,
        )

        components.admin.register(durableCachesAdminModule)

        components.managed.register(heartbeatWorker)

        return ClusterComponent()
    }
}