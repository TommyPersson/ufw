package io.tpersson.ufw.database.unitofwork

import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.useInTransaction
import io.tpersson.ufw.database.typedqueries.TypedUpdate
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement

public class UnitOfWorkImpl(
    private val connectionProvider: ConnectionProvider,
    private val config: DatabaseModuleConfig
) : UnitOfWork {
    private val operations = mutableListOf<Operation>()

    private val postCommitHooks = mutableListOf<suspend () -> Unit>()

    override fun add(minimumAffectedRows: Int, block: Connection.() -> PreparedStatement) {
        operations += Operation.Plain(minimumAffectedRows, block)
    }

    override fun add(update: TypedUpdate) {
        operations += Operation.TypedUpdate(update)
    }

    override fun addPostCommitHook(block: suspend () -> Unit) {
        postCommitHooks += block
    }

    override suspend fun commit() {
        withContext(config.ioContext) {
            connectionProvider.get().useInTransaction {
                for (operation in operations) {
                    val affectedRows = operation.makePreparedStatement(it).executeUpdate()

                    if (affectedRows < operation.minimumAffectedRows) {
                        // TODO custom exception?
                        throw Exception("MinimumAffectedRows not hit")
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
        fun makePreparedStatement(connection: Connection): PreparedStatement

        data class Plain(
            override val minimumAffectedRows: Int,
            val update: Connection.() -> PreparedStatement,
        ) : Operation {
            override fun makePreparedStatement(connection: Connection): PreparedStatement = update(connection)
        }

        data class TypedUpdate(
            val update: io.tpersson.ufw.database.typedqueries.TypedUpdate
        ) : Operation {
            override val minimumAffectedRows = update.minimumAffectedRows
            override fun makePreparedStatement(connection: Connection): PreparedStatement {
                return update.asPreparedStatement(connection)
            }
        }
    }
}