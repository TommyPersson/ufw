package io.tpersson.ufw.core.builder

public object UFW {
    public fun build(builder: UFWBuilder.Root.() -> Unit): ComponentRegistry {
        return UFWBuilder().build(builder)
    }
}