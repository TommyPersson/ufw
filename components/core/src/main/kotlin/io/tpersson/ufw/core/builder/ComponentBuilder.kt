package io.tpersson.ufw.core.builder

public interface ComponentBuilder<out T : Component<*>> {
    public fun build(components: ComponentRegistry): T
}