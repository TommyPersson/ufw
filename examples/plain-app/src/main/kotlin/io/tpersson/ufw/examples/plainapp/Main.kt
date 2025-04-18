package io.tpersson.ufw.examples.plainapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.tpersson.ufw.admin.builder.installAdmin
import io.tpersson.ufw.aggregates.builder.aggregates
import io.tpersson.ufw.aggregates.builder.installAggregates
import io.tpersson.ufw.cluster.builder.installCluster
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.builders.UFW
import io.tpersson.ufw.core.builders.ComponentRegistry
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.database.builder.database
import io.tpersson.ufw.database.builder.installDatabase
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.builder.installDatabaseQueue
import io.tpersson.ufw.durablecaches.builder.durableCaches
import io.tpersson.ufw.durablecaches.builder.installDurableCaches
import io.tpersson.ufw.durableevents.builder.durableEvents
import io.tpersson.ufw.durableevents.builder.installDurableEvents
import io.tpersson.ufw.durablejobs.builder.durableJobs
import io.tpersson.ufw.durablejobs.builder.installDurableJobs
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
import io.tpersson.ufw.examples.common.jobs.periodic.PeriodicPrintJob2Handler
import io.tpersson.ufw.examples.common.jobs.periodic.PeriodicPrintJobHandler
import io.tpersson.ufw.examples.common.managed.PeriodicEventPublisher
import io.tpersson.ufw.examples.common.managed.PeriodicJobScheduler
import io.tpersson.ufw.examples.common.managed.PeriodicLogger
import io.tpersson.ufw.examples.common.managed.PrometheusServer
import io.tpersson.ufw.examples.common.queries.TestAdminQuery1Handler
import io.tpersson.ufw.featuretoggles.builder.featureToggles
import io.tpersson.ufw.featuretoggles.builder.installFeatureToggles
import io.tpersson.ufw.keyvaluestore.builder.keyValueStore
import io.tpersson.ufw.keyvaluestore.builder.installKeyValueStore
import io.tpersson.ufw.managed.builder.managed
import io.tpersson.ufw.managed.builder.installManaged
import io.tpersson.ufw.mediator.builder.mediator
import io.tpersson.ufw.mediator.builder.installMediator
import io.tpersson.ufw.mediator.middleware.transactional.TransactionalMiddleware
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.bridge.SLF4JBridgeHandler
import java.time.Clock
import java.util.*


public fun main(): Unit = runBlocking(MDCContext()) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val ufw = UFW.build {
        installCore { // "withCore", "withManaged"?
            clock = Clock.systemDefaultZone()
            meterRegistry = Globals.meterRegistry
            appInfoProvider = AppInfoProvider.simple(name = "Example (plain)", version = "0.0.1", environment = "dev")
            configProviderFactory = ConfigProvider.Companion::default

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
        }
        installManaged()
        installAdmin()
        installDatabase {
            dataSource = Globals.dataSource
        }
        installDatabaseQueue()
        installKeyValueStore()
        installDurableCaches()
        installMediator()
        installDurableJobs()
        installDurableEvents {
            outgoingEventTransport = null // TODO DirectOutgoingEventTransport()
        }
        installAggregates()
        installFeatureToggles()
        installCluster()
    }

    ufw.durableEvents.register(ExampleDurableEventHandler(ufw.keyValueStore.keyValueStore))

    ufw.durableJobs.register(PrintJobHandler())
    ufw.durableJobs.register(PrintJob2Handler(ufw.mediator.mediator))
    ufw.durableJobs.register(ExpensiveCalculationJobHandler(ufw.durableCaches.durableCaches))
    ufw.durableJobs.register(SensitiveDataRefreshJobHandler(ufw.durableCaches.durableCaches))
    ufw.durableJobs.register(PeriodicPrintJobHandler())
    ufw.durableJobs.register(PeriodicPrintJob2Handler())

    val counterRepository = CounterAggregateRepository(ufw.aggregates)

    ufw.aggregates.register(counterRepository)

    ufw.mediator.register(PerformGreetingCommandHandler(ufw.keyValueStore.keyValueStore))
    ufw.mediator.register(TestAdminCommand1Handler())
    ufw.mediator.register(TestAdminCommand2Handler())
    ufw.mediator.register(TestAdminQuery1Handler())

    ufw.mediator.register(TransactionalMiddleware(ufw.database.unitOfWorkFactory))

    ufw.managed.register(PrometheusServer(Globals.meterRegistry))
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
            jobQueue = ufw.durableJobs.jobQueue,
            featureToggles = ufw.featureToggles.featureToggles,
            unitOfWorkFactory = ufw.database.unitOfWorkFactory,
        )
    )

    ufw.database.runMigrations()

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

private suspend fun testTransactionalEvents(ufw: ComponentRegistry) {
    val transactionalEventPublisher = ufw.durableEvents.eventPublisher
    val unitOfWorkFactory = ufw.database.unitOfWorkFactory

    val event = ExampleEventV1(myContent = "Hello, World!")

    unitOfWorkFactory.use { uow ->
        transactionalEventPublisher.publish(event, uow)
    }
}

private suspend fun testMediator(ufw: ComponentRegistry) {
    val mediator = ufw.mediator.mediator
    mediator.send(PerformGreetingCommand("World"))
}

private suspend fun testJobQueue(ufw: ComponentRegistry) {
    val jobQueue = ufw.durableJobs.jobQueue

    ufw.database.unitOfWorkFactory.use { uow ->
        (1..3).forEach {
            jobQueue.enqueue(PrintJob("$it: Hello, World!"), uow)
            jobQueue.enqueue(PrintJob2("$it: Hello, World!"), uow)
        }
    }
}

private suspend fun testAggregates(ufw: ComponentRegistry, counterRepository: CounterAggregateRepository) {
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
