package io.tpersson.ufw.db.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProvider
import io.tpersson.ufw.db.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactoryImpl
import javax.sql.DataSource


public class DbGuiceModule(
    private val dataSource: DataSource? = null,
    private val config: DbModuleConfig = DbModuleConfig.Default
) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            if (dataSource != null) {
                bind(DataSource::class.java).toInstance(dataSource)
            }

            bind(DbModuleConfig::class.java).toInstance(config)
            bind(ConnectionProvider::class.java).to(ConnectionProviderImpl::class.java)
            bind(UnitOfWorkFactory::class.java).to(UnitOfWorkFactoryImpl::class.java)
        }
    }
}