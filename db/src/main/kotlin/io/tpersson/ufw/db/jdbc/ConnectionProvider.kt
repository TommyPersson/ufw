package io.tpersson.ufw.db.jdbc

import java.sql.Connection

public interface ConnectionProvider {
    public fun get(): Connection
}