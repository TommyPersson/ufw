package io.tpersson.ufw.admin.builder

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.managed.builder.installManaged
import io.tpersson.ufw.managed.builder.managed

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

    public override fun build(components: ComponentRegistry): AdminComponent {
        return AdminComponent.create(
            coreComponent = components.core,
            managedComponent = components.managed,
        )
    }
}

public val ComponentRegistry.admin: AdminComponent get() = get(AdminComponent)