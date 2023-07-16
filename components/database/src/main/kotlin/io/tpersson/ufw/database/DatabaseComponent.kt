package io.tpersson.ufw.database

import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.locks.DatabaseLock
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.locks.internal.DatabaseLocksDAO
import io.tpersson.ufw.database.locks.internal.DatabaseLocksImpl
import io.tpersson.ufw.database.migrations.Migrator
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactoryImpl
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext


public class DatabaseComponent @Inject constructor(
    public val database: Database,
    public val connectionProvider: ConnectionProvider,
    public val unitOfWorkFactory: UnitOfWorkFactory,
    public val migrator: Migrator,
    public val locks: DatabaseLocks,
    public val config: DatabaseModuleConfig,
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
            ioContext: CoroutineContext = Dispatchers.IO
        ): DatabaseComponent {
            val config = DatabaseModuleConfig(
                ioContext = ioContext
            )

            val connectionProvider = ConnectionProviderImpl(
                dataSource = dataSource
            )

            val database = Database(
                connectionProvider = connectionProvider,
                config = config
            )

            val unitOfWorkFactory = UnitOfWorkFactoryImpl(
                connectionProvider = connectionProvider,
                config = config
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
                config = config,
                locks = databaseLocks,
                migrator = migrator
            )
        }
    }
}