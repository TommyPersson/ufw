package io.tpersson.ufw.database.locks

import java.time.Duration

public interface DatabaseLock {
    public suspend fun tryAcquire(stealIfOlderThan: Duration? = null): DatabaseLockHandle?
}