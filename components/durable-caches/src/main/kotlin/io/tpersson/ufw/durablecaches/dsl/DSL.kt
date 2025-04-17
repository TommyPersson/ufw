package io.tpersson.ufw.durablecaches.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.core.builder.*
import io.tpersson.ufw.durablecaches.DurableCachesComponent
import io.tpersson.ufw.keyvaluestore.dsl.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore

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

    public override fun build(components: ComponentRegistry): DurableCachesComponent {
        return DurableCachesComponent.create(
            coreComponent = components.core,
            keyValueStoreComponent = components.keyValueStore,
            adminComponent = components.admin,
        )
    }
}

public val ComponentRegistry.durableCaches: DurableCachesComponent get() = get(DurableCachesComponent)