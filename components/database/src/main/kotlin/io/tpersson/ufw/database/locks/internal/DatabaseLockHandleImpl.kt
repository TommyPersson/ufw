package io.tpersson.ufw.database.locks.internal

import io.tpersson.ufw.database.locks.DatabaseLockHandle

public class DatabaseLockHandleImpl(
    private val doRefresh: suspend () -> Boolean,
    private val doRelease: suspend () -> Unit
) : DatabaseLockHandle {
    override suspend fun refresh(): Boolean {
        return doRefresh()
    }

    override suspend fun release() {
        doRelease()
    }
}