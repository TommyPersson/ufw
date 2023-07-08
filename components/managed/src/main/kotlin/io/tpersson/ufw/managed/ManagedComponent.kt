package io.tpersson.ufw.managed

public class ManagedComponent private constructor(
    public val managedRunner: ManagedRunner
) {
    public companion object {
        public fun create(instances: Set<Managed>): ManagedComponent {
            val managedRunner = ManagedRunner(instances)
            return ManagedComponent(managedRunner)
        }
    }
}