package io.tpersson.ufw.admin.component

import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.internal.AdminModulesRegistry
import io.tpersson.ufw.admin.internal.SimpleAdminModulesRegistry
import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class AdminComponent @Inject constructor(
    private val adminModulesRegistry: AdminModulesRegistry,
) : Component<AdminComponent> {

    public fun register(adminModule: AdminModule) {
        if (adminModulesRegistry is SimpleAdminModulesRegistry) {
            adminModulesRegistry.add(adminModule)
        } else {
            error("Unable to add AdminModule")
        }
    }

    public companion object : ComponentKey<AdminComponent>
}

public val ComponentRegistry.admin: AdminComponent get() = get(AdminComponent)