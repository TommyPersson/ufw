package io.tpersson.ufw.admin

import io.tpersson.ufw.admin.internal.AdminModulesProvider
import io.tpersson.ufw.admin.internal.CoreAdminModule
import io.tpersson.ufw.admin.internal.ManagedAdminServer
import io.tpersson.ufw.admin.internal.SimpleAdminModulesProvider
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.builder.ComponentKey
import io.tpersson.ufw.core.builder.Component
import io.tpersson.ufw.managed.ManagedComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class AdminComponent @Inject constructor(
    private val adminModulesProvider: AdminModulesProvider,
) : Component<AdminComponent> {

    public fun register(adminModule: AdminModule) {
        if (adminModulesProvider is SimpleAdminModulesProvider) {
            adminModulesProvider.add(adminModule)
        } else {
            error("Unable to add AdminModule")
        }
    }

    public companion object : ComponentKey<AdminComponent> {
        public fun create(
            coreComponent: CoreComponent,
            managedComponent: ManagedComponent,
        ): AdminComponent {

            val adminModulesProvider = SimpleAdminModulesProvider().also {
                it.add(CoreAdminModule(it, coreComponent.appInfoProvider))
            }

            val managedAdminServer = ManagedAdminServer(adminModulesProvider, coreComponent.configProvider)

            managedComponent.register(managedAdminServer)

            return AdminComponent(adminModulesProvider)
        }
    }
}