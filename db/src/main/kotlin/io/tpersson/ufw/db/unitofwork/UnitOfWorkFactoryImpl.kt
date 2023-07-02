package io.tpersson.ufw.db.unitofwork

import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProvider
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class UnitOfWorkFactoryImpl @Inject constructor(
    private val connectionProvider: ConnectionProvider,
    private val config: DbModuleConfig,
) : UnitOfWorkFactory {
    override fun create(): UnitOfWork {
        return UnitOfWorkImpl(connectionProvider, config)
    }
}