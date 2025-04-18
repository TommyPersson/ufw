package io.tpersson.ufw.core.components

public interface ComponentBuilder<out T : Component<*>> {
    public fun build(components: ComponentRegistryInternal): T
}