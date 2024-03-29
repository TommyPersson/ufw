package io.tpersson.ufw.core.dsl

@DslMarker
public annotation class UfwDslMarker()

public class UFWBuilder {

    public val components: MutableMap<String, Any> = mutableMapOf()

    @UfwDslMarker
    public fun build(builder: RootBuilder.() -> Unit): UFWRegistry {
        RootBuilder(components).also(builder)

        return UFWRegistry(components)
    }

    @UfwDslMarker
    public inner class RootBuilder(
        public val components: MutableMap<String, Any>,
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