package io.tpersson.ufw.database

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.locks.internal.DatabaseLocksDAO
import io.tpersson.ufw.database.locks.internal.DatabaseLocksImpl
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactoryImpl
import jakarta.inject.Inject
import javax.sql.DataSource


public class DatabaseComponent @Inject constructor(
    public val database: Database,
    public val connectionProvider: ConnectionProvider,
    public val unitOfWorkFactory: UnitOfWorkFactory,
    public val migrator: Migrator,
    public val locks: DatabaseLocks,
) {
    init {
        Migrator.registerMigrationScript(
            componentName = "database",
            scriptLocation = "io/tpersson/ufw/database/migrations/postgres/liquibase.xml"
        )
    }

    public fun runMigrations() {
        migrator.run()
    }

    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            dataSource: DataSource,
        ): DatabaseComponent {
            val connectionProvider = ConnectionProviderImpl(
                dataSource = dataSource
            )

            val database = Database(
                connectionProvider = connectionProvider,
            )

            val unitOfWorkFactory = UnitOfWorkFactoryImpl(
                connectionProvider = connectionProvider,
            )

            val migrator = Migrator(
                connectionProvider = connectionProvider
            )

            val databaseLocks = DatabaseLocksImpl(
                databaseLocksDAO = DatabaseLocksDAO(database = database),
                clock = coreComponent.clock
            )

            return DatabaseComponent(
                database = database,
                connectionProvider = connectionProvider,
                unitOfWorkFactory = unitOfWorkFactory,
                locks = databaseLocks,
                migrator = migrator
            )
        }
    }
}