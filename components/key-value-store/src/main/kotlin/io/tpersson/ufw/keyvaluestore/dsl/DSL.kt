package io.tpersson.ufw.keyvaluestore.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent

@UfwDslMarker
public fun UFWBuilder.RootBuilder.keyValueStore(builder: KeyValueStoreComponentBuilder.() -> Unit) {
    components["KeyValueStore"] = KeyValueStoreComponentBuilder(components).also(builder).build()
}

@UfwDslMarker
public class KeyValueStoreComponentBuilder(
    private val components: MutableMap<String, Any>
) {
    public var objectMapper: ObjectMapper? = null

    public fun build(): KeyValueStoreComponent {
        return KeyValueStoreComponent.create(
            components["core"] as CoreComponent,
            components["database"] as DatabaseComponent
        )
    }
}

public val UFWRegistry.keyValueStore: KeyValueStoreComponent get() = _components["KeyValueStore"] as KeyValueStoreComponent