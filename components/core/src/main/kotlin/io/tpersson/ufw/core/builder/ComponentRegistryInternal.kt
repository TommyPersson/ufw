package io.tpersson.ufw.core.builder

public interface ComponentRegistryInternal : ComponentRegistry {
    public fun <T : Component<*>> set(key: ComponentKey<T>, value: T)
}