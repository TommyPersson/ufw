package io.tpersson.ufw.core.builder

import io.tpersson.ufw.core.components.ComponentRegistry

public object UFW {
    public fun build(builder: UFWBuilder.Root.() -> Unit): ComponentRegistry {
        return UFWBuilder().build(builder)
    }
}