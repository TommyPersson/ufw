package io.tpersson.ufw.keyvaluestore.component

import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.keyvaluestore.KeyValueStoreImpl
import io.tpersson.ufw.keyvaluestore.internal.ExpiredEntryReaper
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.managed.component.installManaged
import io.tpersson.ufw.managed.component.managed

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

    override fun build(components: ComponentRegistryInternal): KeyValueStoreComponent {
        val storageEngine = PostgresStorageEngine(
            unitOfWorkFactory = components.database.unitOfWorkFactory,
            database = components.database.database,
        )

        val keyValueStore = KeyValueStoreImpl(
            storageEngine = storageEngine,
            clock = components.core.clock,
            objectMapper = components.core.objectMapper
        )

        val expiredEntryReaper = ExpiredEntryReaper(
            storageEngine = storageEngine,
            clock = components.core.clock,
            configProvider = components.core.configProvider,
        )

        components.managed.register(expiredEntryReaper)

        return KeyValueStoreComponent(keyValueStore, storageEngine)
    }
}