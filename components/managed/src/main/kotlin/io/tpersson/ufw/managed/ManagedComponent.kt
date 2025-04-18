package io.tpersson.ufw.managed

import io.tpersson.ufw.core.builders.ComponentKey
import io.tpersson.ufw.core.builders.Component
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
        public fun create(): ManagedComponent {
            val managedRunner = ManagedRunner(emptySet())
            return ManagedComponent(managedRunner)
        }
    }
}