package io.tpersson.ufw.examples.plainapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.tpersson.ufw.aggregates.dsl.aggregates
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.core.dsl.objectMapper
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.examples.common.Globals
import io.tpersson.ufw.examples.common.aggregate.CounterAggregate
import io.tpersson.ufw.examples.common.aggregate.CounterAggregateRepository
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

    val ufw = UFW.build {
        core {
            clock = Clock.systemUTC()
            openTelemetry = Globals.openTelemetry

            objectMapper {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }
        managed {
            instances = setOf(
                PeriodicLogger()
            )
        }
        database {
            dataSource = Globals.dataSource
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
    }

    ufw.database.runMigrations()

    ufw.managed.startAll(addShutdownHook = true)

    testMediator(ufw)

    testJobQueue(ufw)

    testAggregates(ufw)

    println("Press Enter to exit")

    val scanner = Scanner(System.`in`)
    scanner.nextLine()

    println("Exiting")

}

private suspend fun testMediator(ufw: UFWRegistry) {
    val mediator = ufw.mediator.mediator
    mediator.send(PerformGreetingCommand("World"))
}

private suspend fun testJobQueue(ufw: UFWRegistry) {
    val jobQueue = ufw.jobQueue.jobQueue

    ufw.database.unitOfWorkFactory.use { uow ->
        jobQueue.enqueue(PrintJob("Hello, World!"), uow)
    }
}

private suspend fun testAggregates(ufw: UFWRegistry) {
    val counterRepository = CounterAggregateRepository(ufw.aggregates)
    val unitOfWorkFactory = ufw.database.unitOfWorkFactory
    val clock = ufw.core.clock

    val counterId = unitOfWorkFactory.use { uow ->
        val counter = CounterAggregate.new(clock.instant())
        counter.increment(clock.instant())
        counterRepository.save(counter, uow)
        counter.id
    }

    run {
        val counter = counterRepository.getById(counterId)!!
        println("CounterAggregate.value = ${counter.value}")
        unitOfWorkFactory.use { uow ->
            counter.increment(clock.instant())
            counterRepository.save(counter, uow)
        }
    }

    run {
        val counter = counterRepository.getById(counterId)!!
        println("CounterAggregate.value = ${counter.value}")
    }
}
