package io.tpersson.ufw.database.unitofwork

import io.tpersson.ufw.database.typedqueries.TypedUpdate
import java.sql.Connection
import java.sql.PreparedStatement

public interface UnitOfWork {
    public fun add(minimumAffectedRows: Int = 1, block: (Connection) -> PreparedStatement)
    public fun add(update: TypedUpdate, exceptionMapper: (Exception) -> Exception = { it })
    public fun addPostCommitHook(block: suspend () -> Unit)
    public suspend fun commit()
}
