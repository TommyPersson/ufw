package io.tpersson.ufw.database.locks.internal

import io.tpersson.ufw.database.locks.DatabaseLock
import io.tpersson.ufw.database.locks.DatabaseLockHandle
import java.time.Duration
import java.time.Clock

public class DatabaseLockImpl(
    private val lockId: String,
    private val instanceId: String,
    private val databaseLocksDAO: DatabaseLocksDAO,
    private val clock: Clock,
) : DatabaseLock {

    override suspend fun tryAcquire(stealIfOlderThan: Duration?): DatabaseLockHandle? {
        val now = clock.instant()

        val isAcquired = databaseLocksDAO.acquireOrRefresh(
            lockId = lockId,
            ownerId = instanceId,
            now = now,
            stealIfOlderThan = stealIfOlderThan
        )

        if (isAcquired) {
            return DatabaseLockHandleImpl(::doRefresh, ::doRelease)
        }

        return null
    }

    private suspend fun doRefresh(): Boolean {
        val now = clock.instant()

        return databaseLocksDAO.acquireOrRefresh(
            lockId = lockId,
            ownerId = instanceId,
            now = now,
            stealIfOlderThan = null
        )
    }

    private suspend fun doRelease() {
        databaseLocksDAO.release(lockId, instanceId)
    }
}