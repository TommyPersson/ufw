package io.tpersson.ufw.admin.component

import io.tpersson.ufw.admin.internal.CoreAdminModule
import io.tpersson.ufw.admin.internal.ManagedAdminServer
import io.tpersson.ufw.admin.internal.SimpleAdminModulesRegistry
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.managed.component.installManaged
import io.tpersson.ufw.managed.component.managed

@UfwDslMarker
public fun UFWBuilder.Root.installAdmin(configure: AdminComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installManaged()

    val ctx = contexts.getOrPut(AdminComponent) { AdminComponentBuilderContext() }
        .also(configure)

    builders.add(AdminComponentBuilder(ctx))
}

public class AdminComponentBuilderContext : ComponentBuilderContext<AdminComponent> {

}

public class AdminComponentBuilder(
    private val context: AdminComponentBuilderContext
) : ComponentBuilder<AdminComponent> {

    public override fun build(components: ComponentRegistryInternal): AdminComponent {
        val adminModulesProvider = SimpleAdminModulesRegistry().also {
            it.add(CoreAdminModule(it, components.core.appInfoProvider))
        }

        val managedAdminServer = ManagedAdminServer(
            adminModulesRegistry = adminModulesProvider,
            configProvider = components.core.configProvider
        )

        components.managed.register(managedAdminServer)

        return AdminComponent(adminModulesProvider)
    }
}