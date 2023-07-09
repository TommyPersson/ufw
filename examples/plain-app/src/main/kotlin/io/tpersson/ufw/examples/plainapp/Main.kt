package io.tpersson.ufw.examples.plainapp

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.aggregates.dsl.aggregates
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommand
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommandHandler
import io.tpersson.ufw.examples.common.jobs.PrintJob
import io.tpersson.ufw.examples.common.jobs.PrintJobHandler
import io.tpersson.ufw.examples.common.managed.PeriodicLogger
import io.tpersson.ufw.jobqueue.dsl.jobQueue
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.mediator.dsl.mediator
import io.tpersson.ufw.mediator.middleware.transactional.TransactionalMiddleware
import java.time.Clock
import java.util.*

public suspend fun main() {

    val hikariConfig = HikariConfig().also {
        it.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
        it.username = "postgres"
        it.password = "postgres"
        it.maximumPoolSize = 30
    }

    val ufw = UFW.build {
        core {
            instantSource = Clock.systemUTC()
        }
        database {
            dataSource = HikariDataSource(hikariConfig)
        }
        keyValueStore {
        }
        mediator {
            handlers = setOf(
                PerformGreetingCommandHandler(components.keyValueStore.keyValueStore)
            )
            middlewares = setOf(
                TransactionalMiddleware(components.database.unitOfWorkFactory)
            )
        }
        jobQueue {
            handlers = setOf(
                PrintJobHandler()
            )
        }
        aggregates {
        }
        managed {
            instances = setOf(
                PeriodicLogger()
            ) + components.jobQueue.managedInstances
        }
    }

    ufw.database.migrator.run()

    val unitOfWorkFactory = ufw.database.unitOfWorkFactory

    val managedRunner = ufw.managed.managedRunner
    managedRunner.startAll()

    val mediator = ufw.mediator.mediator

    mediator.send(PerformGreetingCommand("World"))

    val jobQueue = ufw.jobQueue.jobQueue

    val unitOfWork = unitOfWorkFactory.create()
    jobQueue.enqueue(PrintJob("Hello, World!"), unitOfWork)
    unitOfWork.commit()

    println("Press Enter to exit")

    val scanner = Scanner(System.`in`)
    scanner.nextLine()

    managedRunner.stopAll()
    println("Exiting")

}
