package io.tpersson.ufw.database.unitofwork

import io.tpersson.ufw.database.exceptions.MinimumAffectedRowsException
import io.tpersson.ufw.database.exceptions.TypedUpdateMinimumAffectedRowsException
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.useInTransaction
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement

public class UnitOfWorkImpl(
    private val connectionProvider: ConnectionProvider,
) : UnitOfWork {
    private val operations = mutableListOf<Operation>()

    private val preCommitHooks = mutableListOf<suspend () -> Unit>()
    private val postCommitHooks = mutableListOf<suspend () -> Unit>()

    override fun add(minimumAffectedRows: Int, block: Connection.() -> PreparedStatement) {
        operations += Operation.Plain(minimumAffectedRows, update = block)
    }

    override fun add(update: TypedUpdate, exceptionMapper: (Exception) -> Exception) {
        operations += Operation.TypedUpdate(update, exceptionMapper)
    }

    override fun addPreCommitHook(block: suspend () -> Unit) {
        preCommitHooks += block
    }

    override fun addPostCommitHook(block: suspend () -> Unit) {
        postCommitHooks += block
    }

    override suspend fun commit() {
        for (hook in preCommitHooks) {
            hook.invoke()
        }

        withContext(Dispatchers.IO) {
            connectionProvider.get().useInTransaction {
                for (operation in operations) {
                    try {
                        val affectedRows = operation.makePreparedStatement(it).executeUpdate()
                        if (affectedRows < operation.minimumAffectedRows) {
                            operation.throwMinimumAffectedRowsException(affectedRows)
                        }
                    } catch (e: Exception) {
                        throw operation.exceptionMapper(e)
                    }
                }
            }
        }

        for (hook in postCommitHooks) {
            hook.invoke()
        }
    }

    internal sealed interface Operation {
        val minimumAffectedRows: Int
        val exceptionMapper: (Exception) -> Exception
        fun makePreparedStatement(connection: Connection): PreparedStatement
        fun throwMinimumAffectedRowsException(actual: Int): Nothing

        data class Plain(
            override val minimumAffectedRows: Int,
            override val exceptionMapper: (Exception) -> Exception = { it },
            val update: Connection.() -> PreparedStatement,
        ) : Operation {

            override fun makePreparedStatement(connection: Connection): PreparedStatement = update(connection)

            override fun throwMinimumAffectedRowsException(actual: Int): Nothing {
                throw MinimumAffectedRowsException(
                    expected = minimumAffectedRows,
                    actual = actual)
            }
        }

        data class TypedUpdate(
            val update: io.tpersson.ufw.database.typedqueries.TypedUpdate,
            override val exceptionMapper: (Exception) -> Exception = { it }
        ) : Operation {

            override val minimumAffectedRows = update.minimumAffectedRows

            override fun makePreparedStatement(connection: Connection): PreparedStatement {
                return update.asPreparedStatement(connection)
            }

            override fun throwMinimumAffectedRowsException(actual: Int): Nothing {
                throw TypedUpdateMinimumAffectedRowsException(
                    expected = minimumAffectedRows,
                    actual = actual,
                    query = update
                )
            }
        }
    }
}

