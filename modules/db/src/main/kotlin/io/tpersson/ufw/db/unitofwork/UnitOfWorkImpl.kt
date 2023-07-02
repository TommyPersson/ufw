package io.tpersson.ufw.db.unitofwork

import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProvider
import io.tpersson.ufw.db.jdbc.useInTransaction
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.coroutines.CoroutineContext

public class UnitOfWorkImpl(
    private val connectionProvider: ConnectionProvider,
    private val config: DbModuleConfig
) : UnitOfWork {
    private val operations = mutableListOf<Operation>()

    private val postCommitHooks = mutableListOf<suspend () -> Unit>()

    override fun add(minimumAffectedRows: Int, block: Connection.() -> PreparedStatement) {
        operations += Operation(minimumAffectedRows, block)
    }

    override fun addPostCommitHook(block: suspend () -> Unit) {
        postCommitHooks += block
    }

    override suspend fun commit() {
        withContext(config.ioContext) {
            connectionProvider.get().useInTransaction {
                for (operation in operations) {
                    val affectedRows = operation.update(it).executeUpdate()

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

    internal data class Operation(
        val minimumAffectedRows: Int,
        val update: Connection.() -> PreparedStatement
    )
}