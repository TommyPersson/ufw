package io.tpersson.ufw.managed

public class ManagedComponent private constructor(
    public val managedRunner: ManagedRunner
) {
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