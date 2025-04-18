package io.tpersson.ufw.durablecaches.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.durablecaches.admin.DurableCachesAdminFacadeImpl
import io.tpersson.ufw.durablecaches.admin.DurableCachesAdminModule
import io.tpersson.ufw.durablecaches.internal.DurableCachesImpl
import io.tpersson.ufw.keyvaluestore.component.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.component.keyValueStore

@UfwDslMarker
public fun UFWBuilder.Root.installDurableCaches(configure: DurableCachesComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installKeyValueStore()
    installAdmin()

    val ctx = contexts.getOrPut(DurableCachesComponent) { DurableCachesComponentBuilderContext() }
        .also(configure)

    builders.add(DurableCachesComponentBuilder(ctx))
}

public class DurableCachesComponentBuilderContext : ComponentBuilderContext<DurableCachesComponent>

public class DurableCachesComponentBuilder(
    private val context: DurableCachesComponentBuilderContext,
) : ComponentBuilder<DurableCachesComponent> {

    public override fun build(components: ComponentRegistryInternal): DurableCachesComponent {
        val durableCaches = DurableCachesImpl(
            keyValueStore = components.keyValueStore.keyValueStore,
            clock = components.core.clock,
        )

        val durableCachesAdminModule = DurableCachesAdminModule(
            adminFacade = DurableCachesAdminFacadeImpl(
                durableCaches = durableCaches,
            ),
            objectMapper = components.core.objectMapper,
        )

        components.admin.register(durableCachesAdminModule)

        return DurableCachesComponent(
            durableCaches = durableCaches
        )
    }
}