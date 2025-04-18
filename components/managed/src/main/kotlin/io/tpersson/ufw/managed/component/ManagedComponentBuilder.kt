package io.tpersson.ufw.managed.component

import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.managed.ManagedRunner

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

    override fun build(components: ComponentRegistryInternal): ManagedComponent {
        val managedRunner = ManagedRunner(emptySet())

        return ManagedComponent(managedRunner)
    }
}