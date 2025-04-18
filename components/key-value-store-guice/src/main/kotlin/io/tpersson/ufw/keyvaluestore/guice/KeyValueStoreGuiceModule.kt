package io.tpersson.ufw.keyvaluestore.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.component.KeyValueStoreComponent
import io.tpersson.ufw.keyvaluestore.KeyValueStoreImpl
import io.tpersson.ufw.keyvaluestore.storageengine.PostgresStorageEngine
import io.tpersson.ufw.keyvaluestore.storageengine.StorageEngine


public class KeyValueStoreGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(StorageEngine::class.java).to(PostgresStorageEngine::class.java)
            bind(KeyValueStore::class.java).to(KeyValueStoreImpl::class.java)
            bind(KeyValueStoreComponent::class.java).asEagerSingleton()
        }
    }
}