package io.tpersson.ufw.examples.plainapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.aggregates.dsl.aggregates
import io.tpersson.ufw.core.dsl.UFW
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.core.dsl.objectMapper
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.dsl.databaseQueue
import io.tpersson.ufw.durablecaches.dsl.durableCaches
import io.tpersson.ufw.durableevents.dsl.durableEvents
import io.tpersson.ufw.durablejobs.dsl.durableJobs
import io.tpersson.ufw.durablejobs.dsl.jobQueue
import io.tpersson.ufw.examples.common.Globals
import io.tpersson.ufw.examples.common.aggregate.CounterAggregate
import io.tpersson.ufw.examples.common.aggregate.CounterAggregateRepository
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommand
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommandHandler
import io.tpersson.ufw.examples.common.commands.TestAdminCommand1Handler
import io.tpersson.ufw.examples.common.commands.TestAdminCommand2Handler
import io.tpersson.ufw.examples.common.events.ExampleDurableEventHandler
import io.tpersson.ufw.examples.common.events.ExampleEventV1
import io.tpersson.ufw.examples.common.jobs.*
import io.tpersson.ufw.examples.common.managed.PeriodicEventPublisher
import io.tpersson.ufw.examples.common.managed.PeriodicJobScheduler
import io.tpersson.ufw.examples.common.managed.PeriodicLogger
import io.tpersson.ufw.examples.common.managed.PrometheusServer
import io.tpersson.ufw.examples.common.queries.TestAdminQuery1
import io.tpersson.ufw.examples.common.queries.TestAdminQuery1Handler
import io.tpersson.ufw.featuretoggles.dsl.featureToggles
import io.tpersson.ufw.keyvaluestore.dsl.keyValueStore
import io.tpersson.ufw.managed.dsl.managed
import io.tpersson.ufw.mediator.dsl.mediator
import io.tpersson.ufw.mediator.middleware.transactional.TransactionalMiddleware
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.bridge.SLF4JBridgeHandler
import java.time.Clock
import java.time.Duration
import java.util.*


public fun main(): Unit = runBlocking(MDCContext()) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install()

    val ufw = UFW.build {
        core {
            clock = Clock.systemUTC()
            meterRegistry = Globals.meterRegistry

            objectMapper {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }
        managed {
            instances = setOf(
                PrometheusServer(Globals.meterRegistry),
            )
        }
        admin {
            port = 8081
        }
        database {
            dataSource = Globals.dataSource
        }
        databaseQueue {
        }
        keyValueStore {
            configure {
                expiredEntryReapingInterval = Duration.ofMinutes(1)
            }
        }
        durableCaches {
        }
        mediator {
            handlers = setOf(
                PerformGreetingCommandHandler(components.keyValueStore.keyValueStore), // TODO allow handlers to be registered after build
                TestAdminCommand1Handler(),
                TestAdminCommand2Handler(),
                TestAdminQuery1Handler(),
            )
            middlewares = setOf(
                TransactionalMiddleware(components.database.unitOfWorkFactory)
            )
        }
        durableJobs {
            configure {
                stalenessDetectionInterval = Duration.ofMinutes(1)
                stalenessAge = Duration.ofMinutes(1)
            }
            durableJobHandlers = setOf(
                PrintJobHandler(),
                PrintJob2Handler(),
                ExpensiveCalculationJobHandler(components.durableCaches.durableCaches),
                SensitiveDataRefreshJobHandler(components.durableCaches.durableCaches)
            )
        }
        durableEvents {
            handlers = setOf(
                ExampleDurableEventHandler(components.keyValueStore.keyValueStore)
            )
        }
        aggregates {
        }
        featureToggles {
        }
    }

    ufw.database.runMigrations()

    val counterRepository = CounterAggregateRepository(ufw.aggregates)

    ufw.aggregates.register(counterRepository)

    ufw.managed.register(
        PeriodicLogger(
            featureToggles = ufw.featureToggles.featureToggles
        ),
    )

    ufw.managed.register(
        PeriodicEventPublisher(
            unitOfWorkFactory = ufw.database.unitOfWorkFactory,
            transactionalEventPublisher = ufw.durableEvents.eventPublisher,
            featureToggles = ufw.featureToggles.featureToggles,
            clock = ufw.core.clock
        )
    )

    ufw.managed.register(
        PeriodicJobScheduler(
            jobQueue = ufw.jobQueue.jobQueue,
            featureToggles = ufw.featureToggles.featureToggles,
            unitOfWorkFactory = ufw.database.unitOfWorkFactory,
        )
    )

    ufw.managed.startAll(addShutdownHook = true)

    testMediator(ufw)

    testJobQueue(ufw)

    testAggregates(ufw, counterRepository)

    testTransactionalEvents(ufw)

    println("Press Enter to exit")

    val scanner = Scanner(System.`in`)
    scanner.nextLine()

    println("Exiting")
}

private suspend fun testTransactionalEvents(ufw: UFWRegistry) {
    val transactionalEventPublisher = ufw.durableEvents.eventPublisher
    val unitOfWorkFactory = ufw.database.unitOfWorkFactory

    val event = ExampleEventV1(myContent = "Hello, World!")

    unitOfWorkFactory.use { uow ->
        transactionalEventPublisher.publish(event, uow)
    }
}

private suspend fun testMediator(ufw: UFWRegistry) {
    val mediator = ufw.mediator.mediator
    mediator.send(PerformGreetingCommand("World"))
}

private suspend fun testJobQueue(ufw: UFWRegistry) {
    val jobQueue = ufw.jobQueue.jobQueue

    ufw.database.unitOfWorkFactory.use { uow ->
        (1..3).forEach {
            jobQueue.enqueue(PrintJob("$it: Hello, World!"), uow)
            jobQueue.enqueue(PrintJob2("$it: Hello, World!"), uow)
        }
    }
}

private suspend fun testAggregates(ufw: UFWRegistry, counterRepository: CounterAggregateRepository) {
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
