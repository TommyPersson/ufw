package io.tpersson.ufw.database.configuration

import io.tpersson.ufw.core.configuration.ConfigElement
import io.tpersson.ufw.core.configuration.Configs

public object DatabaseConfigs {
    public val LiquibaseTableName: ConfigElement<String> = ConfigElement.of(
        "database",
        "liquibase-table-name",
        default = "ufw__liquibase"
    )

    public val LiquibaseLockTableName: ConfigElement<String> = ConfigElement.of(
        "database",
        "liquibase-lock-table-name",
        default = "ufw__liquibase_locks"
    )
}

public val Configs.Database: DatabaseConfigs get() = DatabaseConfigs