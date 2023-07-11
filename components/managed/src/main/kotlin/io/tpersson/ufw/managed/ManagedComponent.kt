package io.tpersson.ufw.managed

import jakarta.inject.Inject

public class ManagedComponent @Inject constructor(
    public val managedRunner: ManagedRunner
) {
    public fun register(instance: Managed) {
        managedRunner.register(instance)
    }

    public fun startAll(addShutdownHook: Boolean = true) {
        managedRunner.startAll(addShutdownHook)
    }

    public suspend fun stopAll() {
        managedRunner.stopAll()
    }

    public companion object {
        public fun create(instances: Set<Managed>): ManagedComponent {
            val managedRunner = ManagedRunner(instances)
            return ManagedComponent(managedRunner)
        }
    }
}