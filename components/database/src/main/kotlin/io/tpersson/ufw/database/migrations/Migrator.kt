package io.tpersson.ufw.database.migrations

import io.tpersson.ufw.database.jdbc.ConnectionProvider
import jakarta.inject.Inject
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

public class Migrator @Inject constructor(
    private val connectionProvider: ConnectionProvider
) {
    public companion object {
        private val scripts = mutableListOf<String>()

        public fun registerMigrationScript(script: String) {
            scripts.add(script)
        }
    }

    public fun run() {
        val jdbcConnection = JdbcConnection(connectionProvider.get())

        val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection).also {
            it.databaseChangeLogTableName = "ufw__liquibase_changelog"
            it.databaseChangeLogLockTableName = "ufw__liquibase_changelog__locks"
        }

        for (script in scripts) {
            val resourceAccessor = ClassLoaderResourceAccessor()
            val liquibase = Liquibase(script, resourceAccessor, database)
            liquibase.update(Contexts(), LabelExpression())
        }

        jdbcConnection.close()
    }
}