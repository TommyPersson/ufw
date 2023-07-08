package io.tpersson.ufw.keyvaluestore

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import java.time.Clock
import java.time.InstantSource

public class KeyValueStoreComponent private constructor(
    public val keyValueStore: KeyValueStore,
    public val storageEngine: StorageEngine,
) {
    public companion object {
        public fun create(
            databaseComponent: DatabaseComponent,
            instantSource: InstantSource = Clock.systemUTC(),
            objectMapper: ObjectMapper = defaultObjectMapper,
        ): KeyValueStoreComponent {
            val storageEngine = PostgresStorageEngine(
                databaseComponent.unitOfWorkFactory,
                databaseComponent.connectionProvider,
                databaseComponent.config
            )

            val config = KeyValueStoreModuleConfig(instantSource, objectMapper)

            val keyValueStore = KeyValueStoreImpl(storageEngine, config)

            return KeyValueStoreComponent(keyValueStore, storageEngine)
        }

        public val defaultObjectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
    }
}