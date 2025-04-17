package io.tpersson.ufw.keyvaluestore

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.builder.ComponentKey
import io.tpersson.ufw.core.builder.Component
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.keyvaluestore.internal.ExpiredEntryReaper
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import io.tpersson.ufw.managed.ManagedComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class KeyValueStoreComponent @Inject constructor(
    public val keyValueStore: KeyValueStore,
    public val storageEngine: StorageEngine,
) : Component<KeyValueStoreComponent> {
    init {
        Migrator.registerMigrationScript(
            componentName = "key_value_store",
            scriptLocation = "io/tpersson/ufw/keyvaluestore/migrations/postgres/liquibase.xml"
        )
    }

    public companion object : ComponentKey<KeyValueStoreComponent> {
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            managedComponent: ManagedComponent,
        ): KeyValueStoreComponent {
            val storageEngine = PostgresStorageEngine(
                unitOfWorkFactory = databaseComponent.unitOfWorkFactory,
                database = databaseComponent.database,
            )

            val keyValueStore = KeyValueStoreImpl(
                storageEngine = storageEngine,
                clock = coreComponent.clock,
                objectMapper = coreComponent.objectMapper
            )

            val expiredEntryReaper = ExpiredEntryReaper(
                storageEngine = storageEngine,
                clock = coreComponent.clock,
                configProvider = coreComponent.configProvider,
            )

            managedComponent.register(expiredEntryReaper)

            return KeyValueStoreComponent(keyValueStore, storageEngine)
        }
    }
}
