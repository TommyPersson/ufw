package io.tpersson.ufw.database.unitofwork

import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class UnitOfWorkFactoryImpl @Inject constructor(
    private val connectionProvider: ConnectionProvider,
    private val config: DatabaseModuleConfig,
) : UnitOfWorkFactory {
    override fun create(): UnitOfWork {
        return UnitOfWorkImpl(connectionProvider, config)
    }
}