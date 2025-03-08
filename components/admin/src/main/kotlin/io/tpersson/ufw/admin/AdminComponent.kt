package io.tpersson.ufw.admin

import io.tpersson.ufw.admin.internal.ManagedAdminServer
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.managed.ManagedComponent
import jakarta.inject.Inject

public class AdminComponent @Inject constructor(
    public val config: AdminComponentConfig,
) {
    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            managedComponent: ManagedComponent,
            config: AdminComponentConfig,
        ): AdminComponent {

            val managedAdminServer = ManagedAdminServer(config)

            managedComponent.register(managedAdminServer)

            return AdminComponent(config)
        }
    }
}