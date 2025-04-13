package io.tpersson.ufw.database.jdbc

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.database.exceptions.TypedUpdateMinimumAffectedRowsException
import io.tpersson.ufw.database.typedqueries.*
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.sql.Connection

public class Database @Inject constructor(
    private val connectionProvider: ConnectionProvider,
) {
    public suspend fun <T : Any> select(query: TypedSelectSingle<T>): T? = io {
        connectionProvider.get(autoCommit = true).use {
            it.select(query)
        }
    }

    public suspend fun <T : Any> select(query: TypedSelectList<T>): PaginatedList<T> = io {
        connectionProvider.get(autoCommit = true).use {
            it.select(query)
        }
    }

    public suspend fun update(
        query: TypedUpdate,
        exceptionMapper: (Exception) -> Exception = { it },
        connection: Connection? = null
    ): Int = io {
        try {
            if (connection == null) {
                return@io connectionProvider.get().useInTransaction {
                    update(query, exceptionMapper, it)
                }
            }

            val affectedRows = query.asPreparedStatement(connection).executeUpdate()
            if (affectedRows < query.minimumAffectedRows) {
                throw TypedUpdateMinimumAffectedRowsException(query.minimumAffectedRows, affectedRows, query)
            }

            affectedRows
        } catch (e: Exception) {
            throw exceptionMapper(e)
        }
    }

    public suspend fun <T : Any> update(
        query: TypedUpdateReturningSingle<T>,
        exceptionMapper: (Exception) -> Exception = { it },
        connection: Connection? = null
    ): T? = io {
        try {
            if (connection == null) {
                return@io connectionProvider.get().useInTransaction {
                    update(query, exceptionMapper, it)
                }
            }

            val result = connection.performUpdateReturning(query)
            if (result == null && query.minimumAffectedRows > 0) {
                throw TypedUpdateMinimumAffectedRowsException(query.minimumAffectedRows, 1, query)
            }

            result
        } catch (e: Exception) {
            throw exceptionMapper(e)
        }
    }

    public suspend fun <T : Any> update(
        query: TypedUpdateReturningList<T>,
        exceptionMapper: (Exception) -> Exception = { it },
        connection: Connection? = null
    ): List<T> = io {
        try {
            if (connection == null) {
                return@io connectionProvider.get().useInTransaction {
                    update(query, exceptionMapper, it)
                }
            }

            val result = connection.performUpdateReturningList(query)
            if (result.size < query.minimumAffectedRows) {
                throw TypedUpdateMinimumAffectedRowsException(query.minimumAffectedRows, 1, query)
            }

            result
        } catch (e: Exception) {
            throw exceptionMapper(e)
        }
    }

    private suspend fun <T> io(block: suspend () -> T): T {
        return withContext(currentCoroutineContext() + Dispatchers.IO) {
            block()
        }
    }
}