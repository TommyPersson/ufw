package io.tpersson.ufw.keyvaluestore.guice

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import io.tpersson.ufw.keyvaluestore.KeyValueStoreImpl
import io.tpersson.ufw.keyvaluestore.KeyValueStoreModuleConfig
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine
import java.time.Clock
import java.time.InstantSource


public class KeyValueStoreGuiceModule(
    private val instantSource: InstantSource = Clock.systemUTC(),
    private val objectMapper: ObjectMapper = KeyValueStoreComponent.defaultObjectMapper,
) : Module {
    override fun configure(binder: Binder) {
        val config = KeyValueStoreModuleConfig(instantSource, objectMapper)

        with(binder) {
            bind(KeyValueStoreModuleConfig::class.java).toInstance(config)
            bind(StorageEngine::class.java).to(PostgresStorageEngine::class.java)
            bind(KeyValueStore::class.java).to(KeyValueStoreImpl::class.java)
        }
    }
}