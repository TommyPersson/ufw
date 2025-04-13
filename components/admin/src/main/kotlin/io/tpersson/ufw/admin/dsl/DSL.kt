package io.tpersson.ufw.admin.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.admin(builder: AdminComponentBuilder.() -> Unit = {}) {
    components["Admin"] = AdminComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class AdminComponentBuilder(public val components: UFWRegistry) {

    public fun build(): AdminComponent {
        return AdminComponent.create(
            coreComponent = components.core,
            managedComponent = components.managed,
        )
    }
}

public val UFWRegistry.admin: AdminComponent get() = _components["Admin"] as AdminComponent