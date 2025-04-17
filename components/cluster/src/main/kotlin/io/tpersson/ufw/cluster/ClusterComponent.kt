package io.tpersson.ufw.cluster

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.cluster.admin.ClusterAdminFacadeImpl
import io.tpersson.ufw.cluster.admin.ClusterAdminModule
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.ComponentKey
import io.tpersson.ufw.core.dsl.UFWComponent
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton


@Singleton
public class ClusterComponent @Inject constructor() : UFWComponent<ClusterComponent> {

    public companion object : ComponentKey<ClusterComponent> {
        public fun create(
            coreComponent: CoreComponent,
            keyValueStoreComponent: KeyValueStoreComponent,
            adminComponent: AdminComponent,
        ): ClusterComponent {
            val durableCachesAdminModule = ClusterAdminModule(
                adminFacade = ClusterAdminFacadeImpl(
                    keyValueStore = keyValueStoreComponent.keyValueStore
                )
            )

            adminComponent.register(durableCachesAdminModule)

            return ClusterComponent()
        }
    }
}

