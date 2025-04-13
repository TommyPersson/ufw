package io.tpersson.ufw.database.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.jdbc.ConnectionProvider
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.jdbc.Database
import io.tpersson.ufw.database.locks.DatabaseLocks
import io.tpersson.ufw.database.locks.internal.DatabaseLocksImpl
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactoryImpl


public class DatabaseGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(ConnectionProvider::class.java).to(ConnectionProviderImpl::class.java)
            bind(UnitOfWorkFactory::class.java).to(UnitOfWorkFactoryImpl::class.java)
            bind(Database::class.java)
            bind(DatabaseLocks::class.java).to(DatabaseLocksImpl::class.java)
            bind(DatabaseComponent::class.java)
        }
    }
}