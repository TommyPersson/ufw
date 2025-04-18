package io.tpersson.ufw.core.builders

public interface ComponentRegistryInternal : ComponentRegistry {
    public fun <T : Component<*>> set(key: ComponentKey<T>, value: T)
}