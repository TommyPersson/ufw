package io.tpersson.ufw.database

import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactoryImpl
import kotlinx.coroutines.Dispatchers
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext


public class DatabaseComponent private constructor(
    public val connectionProvider: ConnectionProvider,
    public val unitOfWorkFactory: UnitOfWorkFactory,
    public val config: DatabaseModuleConfig,
) {
    public companion object {
        public fun create(
            dataSource: DataSource,
            ioContext: CoroutineContext = Dispatchers.IO
        ): DatabaseComponent {
            val config = DatabaseModuleConfig(ioContext)
            val connectionProvider = ConnectionProviderImpl(dataSource)
            val unitOfWorkFactory = UnitOfWorkFactoryImpl(connectionProvider, config)

            return DatabaseComponent(
                connectionProvider = connectionProvider,
                unitOfWorkFactory = unitOfWorkFactory,
                config = config,
            )
        }
    }
}