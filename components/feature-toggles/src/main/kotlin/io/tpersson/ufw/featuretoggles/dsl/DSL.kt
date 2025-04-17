package io.tpersson.ufw.featuretoggles.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.core.builder.*
import io.tpersson.ufw.featuretoggles.FeatureTogglesComponent
import io.tpersson.ufw.keyvaluestore.dsl.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore

@UfwDslMarker
public fun UFWBuilder.Root.installFeatureToggles(configure: FeatureTogglesBuilderContext.() -> Unit = {}) {
    installCore()
    installKeyValueStore()
    installAdmin()

    val ctx = contexts.getOrPut(FeatureTogglesComponent) { FeatureTogglesBuilderContext() }
        .apply(configure)

    builders.add(FeatureTogglesComponentBuilder(ctx))
}

public class FeatureTogglesBuilderContext : ComponentBuilderContext<FeatureTogglesComponent>

public class FeatureTogglesComponentBuilder(
    private val context: FeatureTogglesBuilderContext
) : ComponentBuilder<FeatureTogglesComponent> {

    override fun build(components: ComponentRegistry): FeatureTogglesComponent {
        return FeatureTogglesComponent.create(
            coreComponent = components.core,
            keyValueStoreComponent = components.keyValueStore,
            adminComponent = components.admin,
        )
    }
}

public val ComponentRegistry.featureToggles: FeatureTogglesComponent get() = get(FeatureTogglesComponent)