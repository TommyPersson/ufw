package io.tpersson.ufw.db.unitofwork

import java.sql.Connection
import java.sql.PreparedStatement

public interface UnitOfWork {
    public fun add(minimumAffectedRows: Int = 1, block: (Connection) -> PreparedStatement)
    public suspend fun commit()
}

