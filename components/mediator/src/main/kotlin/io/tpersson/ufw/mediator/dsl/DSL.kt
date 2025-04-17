package io.tpersson.ufw.mediator.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.core.builder.*
import io.tpersson.ufw.mediator.MediatorComponent
import io.tpersson.ufw.mediator.MediatorComponentImpl

@UfwDslMarker
public fun UFWBuilder.Root.installMediator(configure: MediatorComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installAdmin()

    val ctx = contexts.getOrPut(MediatorComponentImpl) { MediatorComponentBuilderContext() }
        .also(configure)

    builders.add(MediatorComponentBuilder(ctx))
}

public class MediatorComponentBuilderContext : ComponentBuilderContext<MediatorComponent> {
}

public class MediatorComponentBuilder(
    private val context: MediatorComponentBuilderContext
) : ComponentBuilder<MediatorComponentImpl> {

    override fun build(components: ComponentRegistry): MediatorComponentImpl {
        return MediatorComponentImpl.create(
            coreComponent = components.core,
            adminComponent = components.admin,
        )
    }
}

public val ComponentRegistry.mediator: MediatorComponent get() = get(MediatorComponentImpl)