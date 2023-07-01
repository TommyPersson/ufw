package io.tpersson.ufw.db.jdbc

import java.sql.Connection
import javax.sql.DataSource

public class ConnectionProviderImpl(
    private val dataSource: DataSource
) : ConnectionProvider {
    override fun get(): Connection = dataSource.connection.also {
        it.autoCommit = false
    }
}