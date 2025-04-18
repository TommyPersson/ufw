package io.tpersson.ufw.core.builders

import java.util.concurrent.ConcurrentHashMap

public class ComponentBuildContexts {

    private val entries = ConcurrentHashMap<ComponentKey<*>, ComponentBuilderContext<Any>>()

    public fun <K : ComponentKey<C>, V : ComponentBuilderContext<C>, C : Component<*>> get(key: K): V? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as V?
    }

    public fun <K : ComponentKey<C>, V : ComponentBuilderContext<C>, C : Component<*>> set(key: K, value: V) {
        @Suppress("UNCHECKED_CAST")
        entries[key] = value as ComponentBuilderContext<Any>
    }

    public fun <K : ComponentKey<C>, V : ComponentBuilderContext<C>, C : Component<*>> getOrPut(
        key: K,
        value: () -> V
    ): V {
        @Suppress("UNCHECKED_CAST")
        return entries.getOrPut(key, value as () -> ComponentBuilderContext<Any>) as V
    }
}