package io.tpersson.ufw.db.unitofwork

import io.tpersson.ufw.db.jdbc.ConnectionProvider
import io.tpersson.ufw.db.jdbc.useInTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.coroutines.CoroutineContext

public class UnitOfWorkImpl(
    private val connectionProvider: ConnectionProvider,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : UnitOfWork {
    private val operations = mutableListOf<Operation>()

    override fun add(minimumAffectedRows: Int, block: Connection.() -> PreparedStatement) {
        operations += Operation(minimumAffectedRows, block)
    }

    override suspend fun commit() {
        withContext(coroutineContext) {
            connectionProvider.get().useInTransaction {
                for (operation in operations) {
                    val affectedRows = operation.update(it).executeUpdate()

                    if (affectedRows < operation.minimumAffectedRows) {
                        throw Exception("MinimumAffectedRows not hit")
                    }
                }
            }
        }
    }

    internal data class Operation(
        val minimumAffectedRows: Int,
        val update: Connection.() -> PreparedStatement
    )
}