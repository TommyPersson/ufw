package io.tpersson.ufw.keyvaluestore.dsl

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.*
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import io.tpersson.ufw.managed.ManagedComponent
import io.tpersson.ufw.managed.dsl.installManaged
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.RootBuilder.installKeyValueStore(configure: KeyValueStoreBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()
    installManaged()

    val ctx = contexts.getOrPut(KeyValueStoreComponent) { KeyValueStoreBuilderContext() }
        .also(configure)

    builders.add(KeyValueStoreComponentBuilder(ctx))
}

public class KeyValueStoreBuilderContext : ComponentBuilderContext<KeyValueStoreComponent>

public class KeyValueStoreComponentBuilder(
    private val context: KeyValueStoreBuilderContext
) : ComponentBuilder<KeyValueStoreComponent> {

    override val dependencies: List<ComponentKey<*>> = listOf(
        CoreComponent,
        DatabaseComponent,
        ManagedComponent,
    )

    override fun build(components: UFWComponentRegistry): KeyValueStoreComponent {
        return KeyValueStoreComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            managedComponent = components.managed,
        )
    }
}

public val UFWComponentRegistry.keyValueStore: KeyValueStoreComponent get() = get(KeyValueStoreComponent)