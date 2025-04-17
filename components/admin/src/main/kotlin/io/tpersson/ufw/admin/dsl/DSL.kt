package io.tpersson.ufw.admin.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.managed.ManagedComponent
import io.tpersson.ufw.managed.dsl.ManagedComponentBuilder
import io.tpersson.ufw.managed.dsl.installManaged
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installAdmin(configure: AdminComponentBuilderContext.() -> Unit = {}) {
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

    override val dependencies: List<ComponentKey<*>> = listOf(
        CoreComponent,
        ManagedComponent,
    )

    public override fun build(components: UFWComponentRegistry): AdminComponent {
        return AdminComponent.create(
            coreComponent = components.core,
            managedComponent = components.managed,
        )
    }
}

public val UFWComponentRegistry.admin: AdminComponent get() = get(AdminComponent)