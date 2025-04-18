package io.tpersson.ufw.database

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.builder.ComponentKey
import io.tpersson.ufw.core.builder.Component
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
import jakarta.inject.Singleton
import javax.sql.DataSource


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

    public companion object : ComponentKey<DatabaseComponent> {

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
                connectionProvider = connectionProvider,
                configProvider = coreComponent.configProvider,
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