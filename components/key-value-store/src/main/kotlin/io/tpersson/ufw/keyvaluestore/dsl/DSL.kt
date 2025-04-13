package io.tpersson.ufw.keyvaluestore.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
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

    public fun build(): KeyValueStoreComponent {
        return KeyValueStoreComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            managedComponent = components.managed,
        )
    }
}

public val UFWRegistry.keyValueStore: KeyValueStoreComponent get() = _components["KeyValueStore"] as KeyValueStoreComponent