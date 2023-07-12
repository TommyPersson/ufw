package io.tpersson.ufw.examples.guiceapp

import com.fasterxml.jackson.databind.SerializationFeature
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import io.opentelemetry.api.OpenTelemetry
import io.tpersson.ufw.aggregates.guice.AggregatesGuiceModule
import io.tpersson.ufw.core.CoreGuiceModule
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.guice.DatabaseGuiceModule
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.examples.common.Globals
import io.tpersson.ufw.examples.common.aggregate.CounterAggregate
import io.tpersson.ufw.examples.common.aggregate.CounterAggregateRepository
import io.tpersson.ufw.examples.common.commands.PerformGreetingCommand
import io.tpersson.ufw.examples.common.jobs.PrintJob
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.guice.JobQueueGuiceModule
import io.tpersson.ufw.keyvaluestore.guice.KeyValueStoreGuiceModule
import io.tpersson.ufw.managed.ManagedRunner
import io.tpersson.ufw.managed.guice.ManagedGuiceModule
import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.guice.MediatorGuiceModule
import java.time.Clock
import java.time.Duration
import java.time.InstantSource
import java.util.*
import javax.sql.DataSource

public suspend fun main() {

    val myAppPackages = setOf("io.tpersson.ufw.examples.guiceapp")

    val injector = Guice.createInjector(
        Module {
            it.bind(DataSource::class.java).toInstance(Globals.dataSource)
            it.bind(InstantSource::class.java).toInstance(Clock.systemUTC())
            it.bind(OpenTelemetry::class.java).toInstance(Globals.openTelemetry)
            it.bind(CounterAggregateRepository::class.java)
        },
        CoreGuiceModule(
            scanPackages = myAppPackages,
            configureObjectMapper = {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        ),
        DatabaseGuiceModule(),
        KeyValueStoreGuiceModule(),
        MediatorGuiceModule(),
        JobQueueGuiceModule(
            config = JobQueueConfig(
                stalenessDetectionInterval = Duration.ofSeconds(30)
            )
        ),
        AggregatesGuiceModule(),
        ManagedGuiceModule(),
    )

    val migrator = injector.getInstance(DatabaseComponent::class.java).migrator
    migrator.run()

    val managedRunner = injector.getInstance(ManagedRunner::class.java)
    managedRunner.startAll(addShutdownHook = true)

    testMediator(injector)

    testJobQueue(injector)

    testAggregates(injector)

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
    val jobQueue = injector.getInstance(JobQueue::class.java)
    val unitOfWorkFactory = injector.getInstance(UnitOfWorkFactory::class.java)

    unitOfWorkFactory.use { uow ->
        jobQueue.enqueue(PrintJob("Hello, World!"), uow)
    }
}

private suspend fun testAggregates(injector: Injector) {
    val counterRepository = injector.getInstance(CounterAggregateRepository::class.java)
    val unitOfWorkFactory = injector.getInstance(UnitOfWorkFactory::class.java)
    val clock = injector.getInstance(InstantSource::class.java)

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