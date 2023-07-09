package io.tpersson.ufw.core.dsl

import io.tpersson.ufw.core.CoreComponent
import java.time.Clock
import java.time.InstantSource

@DslMarker
public annotation class UfwDslMarker()

public class UFWBuilder {

    public val components: MutableMap<String, Any> = mutableMapOf()

    public var coreComponent: CoreComponent? = null

    @UfwDslMarker
    public fun build(builder: RootBuilder.() -> Unit): UFWRegistry {
        RootBuilder(components).also(builder)

        return UFWRegistry(components)
    }

    @UfwDslMarker
    public inner class RootBuilder(
        public val components: MutableMap<String, Any>
    )
}

public object UFW {
    public fun build(builder: UFWBuilder.RootBuilder.() -> Unit): UFWRegistry {
        return UFWBuilder().build(builder)
    }
}

public class UFWRegistry(
    public val _components: Map<String, Any>
)

@UfwDslMarker
public fun UFWBuilder.RootBuilder.core(builder: CoreComponentBuilder.() -> Unit) {
    components["core"] = CoreComponentBuilder().also(builder).build()
}

@UfwDslMarker
public class CoreComponentBuilder {
    public var clock: InstantSource = Clock.systemUTC()

    public fun build(): CoreComponent {
        return CoreComponent.create(clock)
    }
}

public val UFWRegistry.core: CoreComponent get() = _components["core"] as CoreComponent