package io.tpersson.ufw.examples.guiceapp

import com.fasterxml.jackson.databind.SerializationFeature
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import io.micrometer.core.instrument.MeterRegistry
import io.tpersson.ufw.admin.guice.AdminGuiceModule
import io.tpersson.ufw.aggregates.guice.AggregatesGuiceModule
import io.tpersson.ufw.cluster.guice.ClusterGuiceModule
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.CoreGuiceModule
import io.tpersson.ufw.database.component.DatabaseComponent
import io.tpersson.ufw.database.guice.DatabaseGuiceModule
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.databasequeue.guice.DatabaseQueueGuiceModule
import io.tpersson.ufw.durablecaches.guice.DurableCachesGuiceModule
import io.tpersson.ufw.durableevents.guice.DurableEventsGuiceModule
import io.tpersson.ufw.durableevents.publisher.DurableEventPublisher
import io.tpersson.ufw.durableevents.publisher.OutgoingEventTransport
import io.tpersson.ufw.durableevents.publisher.transports.DirectOutgoingEventTransport
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.guice.DurableJobsGuiceModule
import io.tpersson.ufw.examples.common.Globals
import io.tpersson.ufw.examples.common.aggregate.CounterAggregate
import io.tpersson.ufw.examples.common.aggregate.CounterAggregateRepository
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommand
import io.tpersson.ufw.examples.common.events.ExampleEventV1
import io.tpersson.ufw.examples.common.jobs.PrintJob
import io.tpersson.ufw.examples.common.jobs.PrintJob2
import io.tpersson.ufw.featuretoggles.guice.FeatureTogglesGuiceModule
import io.tpersson.ufw.keyvaluestore.guice.KeyValueStoreGuiceModule
import io.tpersson.ufw.managed.component.ManagedComponent
import io.tpersson.ufw.managed.guice.ManagedGuiceModule
import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.guice.MediatorGuiceModule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.bridge.SLF4JBridgeHandler
import java.time.Clock
import java.util.*
import javax.sql.DataSource

public fun main(): Unit = runBlocking(MDCContext()) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install()

    val myAppPackages = setOf("io.tpersson.ufw.examples.guiceapp")

    val injector = Guice.createInjector(
        Module {
            it.bind(DataSource::class.java).toInstance(Globals.dataSource)
            it.bind(Clock::class.java).toInstance(Clock.systemDefaultZone())

            OptionalBinder.newOptionalBinder(it, AppInfoProvider::class.java)
                .setBinding().toInstance(
                    AppInfoProvider.simple(name = "Example (guice)", version = "0.0.1", environment = "dev")
                )

            OptionalBinder.newOptionalBinder(it, MeterRegistry::class.java)
                .setBinding().toInstance(Globals.meterRegistry)

            OptionalBinder.newOptionalBinder(it, OutgoingEventTransport::class.java)
                .setBinding().to(DirectOutgoingEventTransport::class.java)

            it.bind(CounterAggregateRepository::class.java)
        },
        CoreGuiceModule(
            scanPackages = myAppPackages,
            configureObjectMapper = {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        ),
        AdminGuiceModule(),
        DatabaseGuiceModule(),
        DatabaseQueueGuiceModule(),
        KeyValueStoreGuiceModule(),
        MediatorGuiceModule(),
        DurableJobsGuiceModule(),
        AggregatesGuiceModule(),
        DurableEventsGuiceModule(),
        FeatureTogglesGuiceModule(),
        DurableCachesGuiceModule(),
        ClusterGuiceModule(),
        ManagedGuiceModule(),
    )

    val migrator = injector.getInstance(DatabaseComponent::class.java).migrator
    migrator.run()

    val managedRunner = injector.getInstance(ManagedComponent::class.java).managedRunner
    managedRunner.startAll(addShutdownHook = true)

    testMediator(injector)

    testJobQueue(injector)

    testAggregates(injector)

    testTransactionalEvents(injector)

    println("Press Enter to exit")

    val scanner = Scanner(System.`in`)
    scanner.nextLine()

    println("Exiting")
}

private suspend fun testMediator(injector: Injector) {
    val mediator = injector.getInstance(Mediator::class.java)
    mediator.send(PerformGreetingCommand("World"))
}

private suspend fun testJobQueue(injector: Injector) {
    val jobQueue = injector.getInstance(DurableJobQueue::class.java)
    val unitOfWorkFactory = injector.getInstance(UnitOfWorkFactory::class.java)

    unitOfWorkFactory.use { uow ->
        (1..3).forEach {
            jobQueue.enqueue(PrintJob("$it: Hello, World!"), uow)
            jobQueue.enqueue(PrintJob2("$it: Hello, World!"), uow)
        }
    }
}

private suspend fun testAggregates(injector: Injector) {
    val counterRepository = injector.getInstance(CounterAggregateRepository::class.java)
    val unitOfWorkFactory = injector.getInstance(UnitOfWorkFactory::class.java)
    val clock = injector.getInstance(Clock::class.java)

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

private suspend fun testTransactionalEvents(injector: Injector) {
    val transactionalEventPublisher = injector.getInstance(DurableEventPublisher::class.java)
    val unitOfWorkFactory = injector.getInstance(UnitOfWorkFactory::class.java)

    val event = ExampleEventV1(myContent = "Hello, World!")

    unitOfWorkFactory.use { uow ->
        transactionalEventPublisher.publish(event, uow)
    }
}