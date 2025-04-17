package io.tpersson.ufw.managed.dsl

import io.tpersson.ufw.core.builder.*
import io.tpersson.ufw.managed.ManagedComponent

@UfwDslMarker
public fun UFWBuilder.Root.installManaged(configure: ManagedComponentBuilderContext.() -> Unit = {}) {
    installCore()

    val ctx = contexts.getOrPut(ManagedComponent) { ManagedComponentBuilderContext() }
        .also(configure)

    builders.add(ManagedComponentBuilder(ctx))
}

public class ManagedComponentBuilderContext : ComponentBuilderContext<ManagedComponent>

public class ManagedComponentBuilder(
    private val context: ManagedComponentBuilderContext
) : ComponentBuilder<ManagedComponent> {

    override fun build(components: ComponentRegistry): ManagedComponent {
        return ManagedComponent.create()
    }
}

public val ComponentRegistry.managed: ManagedComponent get() = get(ManagedComponent)