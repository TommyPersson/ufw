package io.tpersson.ufw.database.locks

public interface DatabaseLockHandle {
    public suspend fun refresh(): Boolean
    public suspend fun release()
}