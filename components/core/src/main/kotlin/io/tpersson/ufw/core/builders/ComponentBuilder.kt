package io.tpersson.ufw.core.builders

public interface ComponentBuilder<out T : Component<*>> {
    public fun build(components: ComponentRegistry): T
}