package io.tpersson.ufw.db.typedqueries

import io.tpersson.ufw.db.typedqueries.internal.TypedQuery
import org.intellij.lang.annotations.Language
import java.sql.*

public abstract class TypedUpdate(
    @Language("sql")
    pseudoSql: String,
    public val minimumAffectedRows: Int = 1,
) : TypedQuery(pseudoSql)

public fun Connection.performUpdate(update: TypedUpdate): Int {
    val statement = update.asPreparedStatement(this)
    return statement.executeUpdate()
}