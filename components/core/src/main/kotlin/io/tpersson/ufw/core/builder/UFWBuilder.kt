package io.tpersson.ufw.core.builder

import io.tpersson.ufw.core.components.ComponentBuildContexts
import io.tpersson.ufw.core.components.ComponentBuilders
import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.core.components.ComponentRegistryImpl
import kotlin.reflect.full.companionObjectInstance

public class UFWBuilder {

    private val contexts = ComponentBuildContexts()
    private val builders = ComponentBuilders()

    private val componentRegistry: ComponentRegistryImpl = ComponentRegistryImpl()

    @UfwDslMarker
    public fun build(builder: Root.() -> Unit): ComponentRegistry {
        Root(contexts, builders).also(builder)

        for (componentBuilder in builders) {
            val component = componentBuilder.build(componentRegistry)
            val key = component::class.companionObjectInstance as ComponentKey<*>
            componentRegistry.set(key, component)
        }

        return componentRegistry
    }

    @UfwDslMarker
    public inner class Root(
        public val contexts: ComponentBuildContexts,
        public val builders: ComponentBuilders
    )
}