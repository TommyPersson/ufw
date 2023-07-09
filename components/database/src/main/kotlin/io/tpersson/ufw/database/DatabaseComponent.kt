package io.tpersson.ufw.database

import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.jdbc.Database
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
    public val config: DatabaseModuleConfig,
) {
    public fun runMigrations() {
        migrator.run()
    }

    public companion object {
        public fun create(
            dataSource: DataSource,
            ioContext: CoroutineContext = Dispatchers.IO
        ): DatabaseComponent {
            val config = DatabaseModuleConfig(ioContext)
            val connectionProvider = ConnectionProviderImpl(dataSource)
            val database = Database(connectionProvider, config)
            val unitOfWorkFactory = UnitOfWorkFactoryImpl(connectionProvider, config)
            val migrator = Migrator(connectionProvider)

            return DatabaseComponent(
                database = database,
                connectionProvider = connectionProvider,
                unitOfWorkFactory = unitOfWorkFactory,
                config = config,
                migrator = migrator
            )
        }
    }
}