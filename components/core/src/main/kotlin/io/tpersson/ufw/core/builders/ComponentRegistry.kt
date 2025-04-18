package io.tpersson.ufw.core.builders

public interface ComponentRegistry {
    public fun <T : Component<*>> get(key: ComponentKey<T>): T
}