package io.tpersson.ufw.core.components

public interface ComponentRegistry {
    public fun <T : Component<*>> get(key: ComponentKey<T>): T
}