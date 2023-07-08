package io.tpersson.ufw.keyvaluestore

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tpersson.ufw.core.Components
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine

public class KeyValueStoreComponent private constructor(
    public val keyValueStore: KeyValueStore,
    public val storageEngine: StorageEngine,
) {
    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            databaseComponent: DatabaseComponent,
            objectMapper: ObjectMapper = defaultObjectMapper,
        ): KeyValueStoreComponent {
            Migrator.registerMigrationScript("io/tpersson/ufw/keyvaluestore/migrations/postgres/liquibase.xml")

            val storageEngine = PostgresStorageEngine(
                databaseComponent.unitOfWorkFactory,
                databaseComponent.connectionProvider,
                databaseComponent.config
            )

            val config = KeyValueStoreModuleConfig(coreComponent.instantSource, objectMapper)

            val keyValueStore = KeyValueStoreImpl(storageEngine, config)

            return KeyValueStoreComponent(keyValueStore, storageEngine)
        }

        public val defaultObjectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
    }
}

@Suppress("UnusedReceiverParameter")
public val Components.KeyValueStore: KeyValueStoreComponent.Companion get() {
    println(this)
  return KeyValueStoreComponent
}