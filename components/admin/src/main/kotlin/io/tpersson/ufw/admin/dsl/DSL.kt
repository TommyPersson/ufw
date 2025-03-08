package io.tpersson.ufw.admin.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.AdminComponentConfig
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.admin(builder: AdminComponentBuilder.() -> Unit) {
    components["Admin"] = AdminComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class AdminComponentBuilder(public val components: UFWRegistry) {

    public var config: AdminComponentConfig = AdminComponentConfig()

    public var port: Int
        get() = config.port
        set(value) {
            config = config.copy(port = value)
        }

    public fun build(): AdminComponent {
        return AdminComponent.create(
            coreComponent = components.core,
            managedComponent = components.managed,
            config = config,
        )
    }
}

public val UFWRegistry.admin: AdminComponent get() = _components["Admin"] as AdminComponent