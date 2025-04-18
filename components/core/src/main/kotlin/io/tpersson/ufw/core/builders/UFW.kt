package io.tpersson.ufw.core.builders

public object UFW {
    public fun build(builder: UFWBuilder.Root.() -> Unit): ComponentRegistry {
        return UFWBuilder().build(builder)
    }
}