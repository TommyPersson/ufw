package io.tpersson.ufw.featuretoggles.builder

import io.tpersson.ufw.admin.builder.admin
import io.tpersson.ufw.admin.builder.installAdmin
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.featuretoggles.FeatureTogglesComponent
import io.tpersson.ufw.keyvaluestore.builder.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.builder.keyValueStore

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