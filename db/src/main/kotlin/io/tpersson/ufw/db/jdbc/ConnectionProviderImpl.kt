package io.tpersson.ufw.db.jdbc

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Connection
import javax.sql.DataSource

@Singleton
public class ConnectionProviderImpl @Inject constructor(
    private val dataSource: DataSource
) : ConnectionProvider {
    override fun get(): Connection = dataSource.connection.also {
        it.autoCommit = false
    }
}