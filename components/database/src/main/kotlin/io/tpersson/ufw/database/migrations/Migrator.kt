package io.tpersson.ufw.database.migrations

import io.tpersson.ufw.database.jdbc.ConnectionProvider
import jakarta.inject.Inject
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.UpdateSummaryEnum
import liquibase.UpdateSummaryOutputEnum
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

public class Migrator @Inject constructor(
    private val connectionProvider: ConnectionProvider
) {
    public companion object {
        private val scripts = mutableListOf<MigrationScript>()

        public fun registerMigrationScript(componentName: String, scriptLocation: String) {
            scripts.add(MigrationScript(componentName, scriptLocation))
        }
    }

    public data class MigrationScript(val componentName: String, val scriptLocation: String)

    public fun run() {
        System.setProperty("liquibase.analytics.enabled", "false")

        val jdbcConnection = JdbcConnection(connectionProvider.get())

        for (script in scripts) {
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection).also {
                it.databaseChangeLogTableName = "ufw__${script.componentName}__liquibase"
                it.databaseChangeLogLockTableName = "ufw__${script.componentName}__liquibase_locks"
            }

            val resourceAccessor = ClassLoaderResourceAccessor()
            val liquibase = Liquibase(script.scriptLocation, resourceAccessor, database)
            liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG)
            liquibase.setShowSummary(UpdateSummaryEnum.OFF)
            liquibase.update(Contexts(), LabelExpression("ufw__${script.componentName}"))
        }

        jdbcConnection.close()
    }
}