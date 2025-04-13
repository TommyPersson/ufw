package io.tpersson.ufw.database.unitofwork

import io.tpersson.ufw.database.jdbc.ConnectionProvider
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class UnitOfWorkFactoryImpl @Inject constructor(
    private val connectionProvider: ConnectionProvider,
) : UnitOfWorkFactory {
    override fun create(): UnitOfWork {
        return UnitOfWorkImpl(connectionProvider)
    }
}