package io.tpersson.ufw.database.component

import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.ComponentRegistry
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import jakarta.inject.Inject
import jakarta.inject.Singleton


@Singleton
public class DatabaseComponent @Inject constructor(
    public val database: Database,
    public val connectionProvider: ConnectionProvider,
    public val unitOfWorkFactory: UnitOfWorkFactory,
    public val migrator: Migrator,
    public val locks: DatabaseLocks,
) : Component<DatabaseComponent> {

    init {
        Migrator.registerMigrationScript(
            componentName = "database",
            scriptLocation = "io/tpersson/ufw/database/migrations/postgres/liquibase.xml"
        )
    }

    public fun runMigrations() {
        migrator.run()
    }

    public companion object : ComponentKey<DatabaseComponent>
}

public val ComponentRegistry.database: DatabaseComponent get() = get(DatabaseComponent)