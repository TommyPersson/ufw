package io.tpersson.ufw.keyvaluestore.builder

import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.database.builder.database
import io.tpersson.ufw.database.builder.installDatabase
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import io.tpersson.ufw.managed.builder.installManaged
import io.tpersson.ufw.managed.builder.managed

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