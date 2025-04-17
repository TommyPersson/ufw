package io.tpersson.ufw.core.dsl

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.companionObjectInstance

@DslMarker
public annotation class UfwDslMarker()

public class UFWBuilder {

    private val components: MutableMap<ComponentKey<*>, Any> = mutableMapOf()
    private val contexts = ComponentBuildContexts(ConcurrentHashMap())
    private val context: ComponentBuildContext = ComponentBuildContext(ConcurrentHashMap()) // TODO delete
    private val builders = ComponentBuilders(LinkedHashMap())

    public val componentRegistry: UFWComponentRegistryImpl = UFWComponentRegistryImpl(components)

    @UfwDslMarker
    public fun build(builder: RootBuilder.() -> Unit): UFWComponentRegistry {
        RootBuilder(componentRegistry, context, contexts, builders).also(builder)

        for (componentBuilder in builders) {
            val component = componentBuilder.build(componentRegistry)
            val key = component::class.companionObjectInstance as ComponentKey<*>
            components[key] = component
        }

        return componentRegistry
    }

    @UfwDslMarker
    public inner class RootBuilder(
        public val components: UFWComponentRegistryInternal,
        public val context: ComponentBuildContext,
        public val contexts: ComponentBuildContexts,
        public val builders: ComponentBuilders
    )
}

public object UFW {
    public fun build(builder: UFWBuilder.RootBuilder.() -> Unit): UFWComponentRegistry {
        return UFWBuilder().build(builder)
    }
}

public class UFWComponentRegistryImpl(
    private val _components: MutableMap<ComponentKey<*>, Any>
) : UFWComponentRegistryInternal {
    public override fun <T : UFWComponent<*>> get(key: ComponentKey<T>): T {
        @Suppress("UNCHECKED_CAST")
        return _components[key] as T? ?: error("'${key}' not installed!")
    }

    public override fun <T : UFWComponent<*>> set(key: ComponentKey<T>, value: T) {
        _components[key] = value as Any
    }
}

public interface UFWComponentRegistry {
    public fun <T : UFWComponent<*>> get(key: ComponentKey<T>): T
}

public interface UFWComponentRegistryInternal : UFWComponentRegistry {
    public fun <T : UFWComponent<*>> set(key: ComponentKey<T>, value: T)
}


public interface ComponentKey<out TComponent : UFWComponent<*>>

public data class ComponentBuildContext(
    private val entries: ConcurrentHashMap<ComponentBuildContextKey<*>, Any>
) {
    public fun <K : ComponentBuildContextKey<V>, V> get(key: K): V? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as V?
    }

    public fun <K : ComponentBuildContextKey<V>, V> put(key: K, value: V) {
        entries[key] = value as Any
    }

    public fun <K : ComponentBuildContextKey<V>, V> getOrPut(key: K, value: () -> V): V {
        @Suppress("UNCHECKED_CAST")
        return entries.getOrPut(key, value) as V
    }
}

public data class ComponentBuildContexts(
    private val entries: ConcurrentHashMap<ComponentKey<*>, ComponentBuilderContext<Any>>
) {
    public fun <K : ComponentKey<C>, V : ComponentBuilderContext<C>, C : UFWComponent<*>> get(key: K): V? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as V?
    }

    public fun <K : ComponentKey<C>, V : ComponentBuilderContext<C>, C : UFWComponent<*>> set(key: K, value: V) {
        @Suppress("UNCHECKED_CAST")
        entries[key] = value as ComponentBuilderContext<Any>
    }

    public fun <K : ComponentKey<C>, V : ComponentBuilderContext<C>, C : UFWComponent<*>> getOrPut(
        key: K,
        value: () -> V
    ): V {
        @Suppress("UNCHECKED_CAST")
        return entries.getOrPut(key, value as () -> ComponentBuilderContext<Any>) as V
    }
}

public class ComponentBuilders(
    private val entries: MutableMap<ComponentKey<*>, ComponentBuilder<*>>
) : Iterable<ComponentBuilder<UFWComponent<Any>>> {
    public fun <K : ComponentKey<C>, V : ComponentBuilder<C>, C : UFWComponent<*>> get(key: K): V? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as V?
    }

    public fun <K : ComponentKey<C>, V : ComponentBuilder<C>, C : UFWComponent<*>> add(value: V) {
        @Suppress("UNCHECKED_CAST")
        val key = ( // TODO simplify
                value::class.allSupertypes
                    .first { it.classifier == ComponentBuilder::class }
                    .arguments.get(0)!!
                    .type!!.classifier as KClass<Any>
                ).companionObjectInstance as K?

        entries[key!!] = value
    }

    override fun iterator(): Iterator<ComponentBuilder<UFWComponent<Any>>> {
        @Suppress("UNCHECKED_CAST")
        return entries.values.iterator() as Iterator<ComponentBuilder<UFWComponent<Any>>>
    }
}

public interface UFWComponent<out T>

public interface ComponentBuildContextKey<out TValue>

public interface ComponentBuilder<out T : UFWComponent<*>> {
    public val dependencies: List<ComponentKey<*>> get() = emptyList()

    public fun build(components: UFWComponentRegistry): T
}

public interface ComponentBuilderContext<TComponent>