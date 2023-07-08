package io.tpersson.ufw.examples.plainapp

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommand
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommandHandler
import io.tpersson.ufw.examples.common.managed.PeriodicLogger
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import io.tpersson.ufw.managed.ManagedComponent
import io.tpersson.ufw.mediator.MediatorComponent
import io.tpersson.ufw.mediator.middleware.transactional.TransactionalMiddleware
import java.util.*

public suspend fun main() {

    val hikariConfig = HikariConfig().also {
        it.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
        it.username = "postgres"
        it.password = "postgres"
        it.maximumPoolSize = 30
    }

    val dataSource = HikariDataSource(hikariConfig)

    val databaseComponent = DatabaseComponent.create(dataSource)

    val keyValueStoreComponent = KeyValueStoreComponent.create(databaseComponent)

    val mediatorComponent = MediatorComponent.create(
        handlers = listOf(
            PerformGreetingCommandHandler(keyValueStoreComponent.keyValueStore)
        ),
        middlewares = listOf(
            TransactionalMiddleware(databaseComponent.unitOfWorkFactory)
        )
    )

    val managedComponent = ManagedComponent.create(
        instances = setOf(
            PeriodicLogger()
        )
    )

    val managedRunner = managedComponent.managedRunner
    managedRunner.startAll()

    val mediator = mediatorComponent.mediator

    mediator.send(PerformGreetingCommand("World"))

    println("Press Enter to exit")

    val scanner = Scanner(System.`in`)
    scanner.nextLine()

    managedRunner.stopAll()
    println("Exiting")
}