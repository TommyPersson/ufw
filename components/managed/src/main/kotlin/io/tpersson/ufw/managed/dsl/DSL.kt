package io.tpersson.ufw.managed.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.managed.Managed
import io.tpersson.ufw.managed.ManagedComponent

@UfwDslMarker
public fun UFWBuilder.RootBuilder.managed(builder: ManagedComponentBuilder.() -> Unit = {}) {
    components["Managed"] = ManagedComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class ManagedComponentBuilder(public val components: UFWRegistry) {
    public var instances: Set<Managed> = emptySet()
    public fun build(): ManagedComponent {
        return ManagedComponent.create(instances)
    }
}

public val UFWRegistry.managed: ManagedComponent get() = _components["Managed"] as ManagedComponent