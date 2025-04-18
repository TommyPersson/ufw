package io.tpersson.ufw.core.builders


public class ComponentRegistryImpl : ComponentRegistryInternal {

    private val components = mutableMapOf<ComponentKey<*>, Any>()

    public override fun <T : Component<*>> get(key: ComponentKey<T>): T {
        @Suppress("UNCHECKED_CAST")
        return components[key] as T? ?: error("'${key}' not installed!")
    }

    public override fun <T : Component<*>> set(key: ComponentKey<T>, value: T) {
        components[key] = value as Any
    }
}