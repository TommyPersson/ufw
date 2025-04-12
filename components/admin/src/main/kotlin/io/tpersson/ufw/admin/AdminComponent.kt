package io.tpersson.ufw.admin

import io.tpersson.ufw.admin.internal.AdminModulesProvider
import io.tpersson.ufw.admin.internal.CoreAdminModule
import io.tpersson.ufw.admin.internal.ManagedAdminServer
import io.tpersson.ufw.admin.internal.SimpleAdminModulesProvider
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.managed.ManagedComponent
import jakarta.inject.Inject

public class AdminComponent @Inject constructor(
    public val config: AdminComponentConfig,
    private val adminModulesProvider: AdminModulesProvider,
    private val server: ManagedAdminServer,
) {
    public fun register(adminModule: AdminModule) {
        if (adminModulesProvider is SimpleAdminModulesProvider) {
            adminModulesProvider.add(adminModule)
        } else {
            error("Unable to add AdminModule")
        }
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            managedComponent: ManagedComponent,
            config: AdminComponentConfig,
        ): AdminComponent {

            val adminModulesProvider = SimpleAdminModulesProvider().also {
                it.add(CoreAdminModule(it, coreComponent.appInfoProvider))
            }

            val managedAdminServer = ManagedAdminServer(config, adminModulesProvider)

            managedComponent.register(managedAdminServer)

            return AdminComponent(config, adminModulesProvider, managedAdminServer)
        }
    }
}