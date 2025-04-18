package io.tpersson.ufw.database.locks

public interface DatabaseLocks {
    public fun create(lockId: String, instanceId: String? = null): DatabaseLock
}
