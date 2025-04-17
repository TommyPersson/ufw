package io.tpersson.ufw.managed.dsl

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.managed.ManagedComponent

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installManaged(configure: ManagedComponentBuilderContext.() -> Unit = {}) {
    installCore()

    val ctx = contexts.getOrPut(ManagedComponent) { ManagedComponentBuilderContext() }
        .also(configure)

    builders.add(ManagedComponentBuilder(ctx))
}

public class ManagedComponentBuilderContext : ComponentBuilderContext<ManagedComponent>

public class ManagedComponentBuilder(
    private val context: ManagedComponentBuilderContext
) : ComponentBuilder<ManagedComponent> {

    override val dependencies: List<ComponentKey<UFWComponent<Any>>> = listOf(CoreComponent)

    override fun build(components: UFWComponentRegistry): ManagedComponent {
        return ManagedComponent.create()
    }
}

public val UFWComponentRegistry.managed: ManagedComponent get() = get(ManagedComponent)