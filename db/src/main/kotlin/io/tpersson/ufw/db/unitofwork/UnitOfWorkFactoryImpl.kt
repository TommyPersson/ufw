package io.tpersson.ufw.db.unitofwork

import io.tpersson.ufw.db.jdbc.ConnectionProvider
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

public class UnitOfWorkFactoryImpl(
    private val connectionProvider: ConnectionProvider,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : UnitOfWorkFactory {
    override fun create(): UnitOfWork {
        return UnitOfWorkImpl(connectionProvider, coroutineContext)
    }
}