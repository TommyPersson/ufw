package io.tpersson.ufw.examples.guiceapp

import com.google.inject.Guice
import com.google.inject.Module
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.examples.guiceapp.commands.PerformGreetingCommand
import io.tpersson.ufw.db.guice.DbGuiceModule
import io.tpersson.ufw.keyvaluestore.guice.KeyValueStoreGuiceModule
import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.guice.MediatorGuiceModule
import io.tpersson.ufw.mediator.guice.MediatorModuleConfig
import javax.sql.DataSource

public suspend fun main() {

    val hikariConfig = HikariConfig().also {
        it.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
        it.username = "postgres"
        it.password = "postgres"
        it.maximumPoolSize = 30
    }

    val dataSource = HikariDataSource(hikariConfig)

    val injector = Guice.createInjector(
        Module { it.bind(DataSource::class.java).toInstance(dataSource) },
        DbGuiceModule(),
        KeyValueStoreGuiceModule(),
        MediatorGuiceModule(MediatorModuleConfig(scanPackages = listOf("io.tpersson.ufw.examples.guiceapp")))
    )

    val mediator = injector.getInstance(Mediator::class.java)

    mediator.send(PerformGreetingCommand("World"))
}