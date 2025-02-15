package io.tpersson.ufw.database.typedqueries.internal

import io.tpersson.ufw.core.reflection.valueByPath
import io.tpersson.ufw.database.jdbc.setParameters
import org.intellij.lang.annotations.Language
import java.sql.*
import java.util.concurrent.ConcurrentHashMap

public abstract class TypedQuery(
    @Language("sql")
    public val pseudoSql: String
) {
    public companion object {
        private val preparedSqlCache = ConcurrentHashMap<String, PreparedSql>()
    }

    private val preparedSql: PreparedSql = preparedSqlCache.getOrPut(pseudoSql) { PreparedSql(pseudoSql) }

    public fun asPreparedStatement(connection: Connection): PreparedStatement {
        val statement = connection.prepareStatement(preparedSql.rawSql)
        val typesAndValues = preparedSql.argNames.map { valueByPath<Any?>(it, this) }

        statement.setParameters(typesAndValues)

        return statement
    }

    public data class PreparedSql(
        @Language("sql")
        val pseudoSql: String,
        val rawSql: String,
        val argNames: List<String>
    ) {
        public companion object {
            private val regex = Regex("(?<!:):[a-zA-Z]+(\\.[a-zA-Z]+)*")
        }

        public constructor(pseudoSql: String) : this(
            pseudoSql = pseudoSql,
            rawSql = regex.replace(pseudoSql, "?"),
            argNames = regex.findAll(pseudoSql).toList().map { it.value.trimStart(':') }
        )
    }
}