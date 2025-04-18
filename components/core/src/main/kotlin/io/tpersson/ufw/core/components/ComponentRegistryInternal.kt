package io.tpersson.ufw.core.components

public interface ComponentRegistryInternal : ComponentRegistry {
    public fun <T : Component<*>> set(key: ComponentKey<T>, value: T)
}