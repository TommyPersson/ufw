package io.tpersson.ufw.durablecaches.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.durablecaches.DurableCachesComponent
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore

@UfwDslMarker
public fun UFWBuilder.RootBuilder.durableCaches(builder: DurableCachesComponentBuilder.() -> Unit = {}) {
    components["DurableCaches"] = DurableCachesComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class DurableCachesComponentBuilder(public val components: UFWRegistry) {
    public fun build(): DurableCachesComponent {
        return DurableCachesComponent.create(
            coreComponent = components.core,
            keyValueStoreComponent = components.keyValueStore,
            adminComponent = components.admin,
        )
    }
}

public val UFWRegistry.durableCaches: DurableCachesComponent get() = _components["DurableCaches"] as DurableCachesComponent