package io.tpersson.ufw.database.jdbc

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Connection
import javax.sql.DataSource

@Singleton
public class ConnectionProviderImpl @Inject constructor(
    override val dataSource: DataSource
) : ConnectionProvider {
    override fun get(autoCommit: Boolean): Connection = dataSource.connection.also {
        it.autoCommit = autoCommit
    }
}