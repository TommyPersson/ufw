package io.tpersson.ufw.database.jdbc

import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.typedqueries.TypedSelect
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.typedqueries.selectList
import io.tpersson.ufw.database.typedqueries.selectSingle
import jakarta.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.sql.Connection

public class Database @Inject constructor(
    private val connectionProvider: ConnectionProvider,
    private val config: DatabaseModuleConfig
) {
    public suspend fun <T : Any> select(query: TypedSelect<T>): T? = io {
        connectionProvider.get(autoCommit = true).use {
            it.selectSingle(query)
        }
    }

    public suspend fun <T : Any> selectList(query: TypedSelect<T>): List<T> = io {
        connectionProvider.get(autoCommit = true).use {
            it.selectList(query)
        }
    }

    public suspend fun update(query: TypedUpdate, connection: Connection? = null): Int = io {
        if (connection == null) {
            return@io connectionProvider.get().useInTransaction {
                update(query, it)
            }
        }

        val affectedRows = query.asPreparedStatement(connection).executeUpdate()
        if (affectedRows < query.minimumAffectedRows) {
            error("minimum affected rows")
        }

        affectedRows
    }

    private suspend fun <T> io(block: suspend () -> T): T {
        return withContext(currentCoroutineContext() + config.ioContext) {
            block()
        }
    }
}