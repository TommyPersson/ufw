package io.tpersson.ufw.featuretoggles.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.featuretoggles.FeatureTogglesComponent
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore

@UfwDslMarker
public fun UFWBuilder.RootBuilder.featureToggles(builder: FeatureTogglesComponentBuilder.() -> Unit = {}) {
    components["FeatureToggles"] = FeatureTogglesComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class FeatureTogglesComponentBuilder(public val components: UFWRegistry) {
    public fun build(): FeatureTogglesComponent {
        return FeatureTogglesComponent.create(
            coreComponent = components.core,
            keyValueStoreComponent = components.keyValueStore,
            adminComponent = components.admin,
        )
    }
}

public val UFWRegistry.featureToggles: FeatureTogglesComponent get() = _components["FeatureToggles"] as FeatureTogglesComponent