package io.tpersson.ufw.durablecaches.dsl

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.durablecaches.DurableCachesComponent
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore
import io.tpersson.ufw.keyvaluestore.dsl.installKeyValueStore

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installDurableCaches(configure: DurableCachesComponentBuilderContext.() -> Unit = {}) {
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

    override val dependencies: List<ComponentKey<*>> = listOf(
        CoreComponent,
        KeyValueStoreComponent,
        AdminComponent
    )

    public override fun build(components: UFWComponentRegistry): DurableCachesComponent {
        return DurableCachesComponent.create(
            coreComponent = components.core,
            keyValueStoreComponent = components.keyValueStore,
            adminComponent = components.admin,
        )
    }
}

public val UFWComponentRegistry.durableCaches: DurableCachesComponent get() = get(DurableCachesComponent)