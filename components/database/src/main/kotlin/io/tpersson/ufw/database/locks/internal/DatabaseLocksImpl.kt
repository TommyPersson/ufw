package io.tpersson.ufw.database.locks.internal

import io.tpersson.ufw.database.locks.DatabaseLock
import io.tpersson.ufw.database.locks.DatabaseLocks
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock

@Singleton
public class DatabaseLocksImpl @Inject constructor(
    private val databaseLocksDAO: DatabaseLocksDAO,
    private val clock: Clock
) : DatabaseLocks {
    override fun create(lockId: String, instanceId: String): DatabaseLock {
        return DatabaseLockImpl(lockId, instanceId, databaseLocksDAO, clock)
    }

    public suspend fun debugDeleteAll() {
        databaseLocksDAO.truncate()
    }
}

