package io.tpersson.ufw.featuretoggles.component

import io.tpersson.ufw.admin.component.admin
import io.tpersson.ufw.admin.component.installAdmin
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.components.*
import io.tpersson.ufw.core.component.core
import io.tpersson.ufw.featuretoggles.admin.FeatureTogglesAdminFacadeImpl
import io.tpersson.ufw.featuretoggles.admin.FeatureTogglesAdminModule
import io.tpersson.ufw.featuretoggles.internal.FeatureTogglesImpl
import io.tpersson.ufw.keyvaluestore.component.installKeyValueStore
import io.tpersson.ufw.keyvaluestore.component.keyValueStore

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

    override fun build(components: ComponentRegistryInternal): FeatureTogglesComponent {
        val featureToggles = FeatureTogglesImpl(
            keyValueStore = components.keyValueStore.keyValueStore,
            clock = components.core.clock,
        )

        val featureTogglesAdminFacade = FeatureTogglesAdminFacadeImpl(
            featureToggles = featureToggles,
            keyValueStore = components.keyValueStore.keyValueStore,
        )

        val featureTogglesAdminModule = FeatureTogglesAdminModule(
            featureTogglesAdminFacade = featureTogglesAdminFacade
        )

        components.admin.register(featureTogglesAdminModule)

        return FeatureTogglesComponent(
            featureToggles = featureToggles
        )
    }
}