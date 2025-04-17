package io.tpersson.ufw.core.builder

public interface ComponentRegistry {
    public fun <T : Component<*>> get(key: ComponentKey<T>): T
}