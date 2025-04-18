package io.tpersson.ufw.database.migrations

import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.database.configuration.Database
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import jakarta.inject.Inject
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.UpdateSummaryEnum
import liquibase.UpdateSummaryOutputEnum
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.ResourceAccessor

public class Migrator @Inject constructor(
    private val connectionProvider: ConnectionProvider,
    private val configProvider: ConfigProvider,
) {
    private val liquibaseTableName = configProvider.get(Configs.Database.LiquibaseTableName)
    private val liquibaseLockTableName = configProvider.get(Configs.Database.LiquibaseLockTableName)

    public companion object {
        private val scripts = mutableListOf<MigrationScript>()

        public fun registerMigrationScript(componentName: String, scriptLocation: String) {
            scripts.add(MigrationScript(componentName, scriptLocation))
        }
    }

    public data class MigrationScript(val componentName: String, val scriptLocation: String)

    public fun run() {
        System.setProperty("liquibase.analytics.enabled", "false")

        JdbcConnection(connectionProvider.get()).use { connection ->
            val database = createLiquibaseDatabase(connection)
            val resourceAccessor = ClassLoaderResourceAccessor()

            for (script in scripts) {
                runScript(database, resourceAccessor, script)
            }
        }
    }

    private fun runScript(
        database: Database,
        resourceAccessor: ResourceAccessor,
        script: MigrationScript
    ) {
        val liquibase = Liquibase(script.scriptLocation, resourceAccessor, database).also {
            it.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG)
            it.setShowSummary(UpdateSummaryEnum.OFF)
        }

        validateChangesets(liquibase)

        liquibase.update(Contexts(), LabelExpression("ufw"))
    }

    private fun validateChangesets(liquibase: Liquibase) {
        for (changeset in liquibase.databaseChangeLog.changeSets) {
            val labels = changeset.labels.toString()
            require(labels.contains("ufw")) {
                "The changeset '${changeset.filePath}' lacks the 'ufw' label."
            }
        }
    }

    private fun createLiquibaseDatabase(connection: JdbcConnection): Database {
        return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection).also {
            it.databaseChangeLogTableName = liquibaseTableName
            it.databaseChangeLogLockTableName = liquibaseLockTableName
        }
    }
}