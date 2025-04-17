package io.tpersson.ufw.keyvaluestore.dsl

import io.tpersson.ufw.core.builder.*
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import io.tpersson.ufw.managed.dsl.installManaged
import io.tpersson.ufw.managed.dsl.managed

@UfwDslMarker
public fun UFWBuilder.Root.installKeyValueStore(configure: KeyValueStoreBuilderContext.() -> Unit = {}) {
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

    override fun build(components: ComponentRegistry): KeyValueStoreComponent {
        return KeyValueStoreComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            managedComponent = components.managed,
        )
    }
}

public val ComponentRegistry.keyValueStore: KeyValueStoreComponent get() = get(KeyValueStoreComponent)