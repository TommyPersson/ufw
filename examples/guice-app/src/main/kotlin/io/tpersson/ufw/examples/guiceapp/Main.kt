package io.tpersson.ufw.examples.guiceapp

import com.google.inject.Guice
import com.google.inject.Module
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.CoreGuiceModule
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommand
import io.tpersson.ufw.database.guice.DatabaseGuiceModule
import io.tpersson.ufw.jobqueue.guice.JobQueueGuiceModule
import io.tpersson.ufw.keyvaluestore.guice.KeyValueStoreGuiceModule
import io.tpersson.ufw.managed.ManagedRunner
import io.tpersson.ufw.managed.guice.internal.ManagedGuiceModule
import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.guice.MediatorGuiceModule
import java.time.Clock
import java.time.InstantSource
import java.util.Scanner
import javax.sql.DataSource

public suspend fun main() {

    val hikariConfig = HikariConfig().also {
        it.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
        it.username = "postgres"
        it.password = "postgres"
        it.maximumPoolSize = 30
    }

    val dataSource = HikariDataSource(hikariConfig)

    val myAppPackages = listOf("io.tpersson.ufw.examples.guiceapp")

    val injector = Guice.createInjector(
        Module {
            it.bind(DataSource::class.java).toInstance(dataSource)
            it.bind(InstantSource::class.java).toInstance(Clock.systemUTC())
        },
        CoreGuiceModule(),
        DatabaseGuiceModule(),
        KeyValueStoreGuiceModule(),
        MediatorGuiceModule(scanPackages = myAppPackages),
        JobQueueGuiceModule(scanPackages = myAppPackages),
        ManagedGuiceModule(scanPackages = myAppPackages)
    )

    val managedRunner = injector.getInstance(ManagedRunner::class.java)
    managedRunner.startAll()

    val mediator = injector.getInstance(Mediator::class.java)
    mediator.send(PerformGreetingCommand("World"))

    println("Press Enter to exit")

    val scanner = Scanner(System.`in`)
    scanner.nextLine()

    managedRunner.stopAll()
    println("Exiting")
}