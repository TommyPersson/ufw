package io.tpersson.ufw.keyvaluestore.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import io.tpersson.ufw.keyvaluestore.KeyValueStoreConfig
import io.tpersson.ufw.managed.dsl.managed
import java.time.Duration

@UfwDslMarker
public fun UFWBuilder.RootBuilder.keyValueStore(builder: KeyValueStoreComponentBuilder.() -> Unit) {
    components["KeyValueStore"] = KeyValueStoreComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class KeyValueStoreComponentBuilder(
    private val components: UFWRegistry
) {
    public var objectMapper: ObjectMapper? = null
    public var config: KeyValueStoreConfig = KeyValueStoreConfig()

    public fun configure(builder: KeyValueStoreConfigBuilder.() -> Unit) {
        config = KeyValueStoreConfigBuilder().also(builder).build()
    }

    public fun build(): KeyValueStoreComponent {
        return KeyValueStoreComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            managedComponent = components.managed,
            config = config,
        )
    }
}


@UfwDslMarker
public class KeyValueStoreConfigBuilder {
    public var expiredEntryReapingInterval: Duration = KeyValueStoreConfig.default.expiredEntryReapingInterval

    internal fun build(): KeyValueStoreConfig {
        return KeyValueStoreConfig(
            expiredEntryReapingInterval = expiredEntryReapingInterval,
        )
    }
}

public val UFWRegistry.keyValueStore: KeyValueStoreComponent get() = _components["KeyValueStore"] as KeyValueStoreComponent