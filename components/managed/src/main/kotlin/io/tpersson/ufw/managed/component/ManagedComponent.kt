package io.tpersson.ufw.managed.component

import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.managed.Managed
import io.tpersson.ufw.managed.ManagedRunner
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class ManagedComponent @Inject constructor(
    public val managedRunner: ManagedRunner
) : Component<ManagedComponent> {

    public fun register(instance: Managed) {
        managedRunner.register(instance)
    }

    public fun startAll(addShutdownHook: Boolean = true) {
        managedRunner.startAll(addShutdownHook)
    }

    public suspend fun stopAll() {
        managedRunner.stopAll()
    }

    public companion object : ComponentKey<ManagedComponent> {
    }
}

public val ComponentRegistry.managed: ManagedComponent get() = get(ManagedComponent)