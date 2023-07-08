package io.tpersson.ufw.database.jdbc

import java.sql.Connection
import javax.sql.DataSource

public interface ConnectionProvider {
    public val dataSource: DataSource
    public fun get(): Connection
}