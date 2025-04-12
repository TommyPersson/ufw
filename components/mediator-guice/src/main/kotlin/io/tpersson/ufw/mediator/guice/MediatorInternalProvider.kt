package io.tpersson.ufw.mediator.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.micrometer.core.instrument.MeterRegistry
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.RequestHandler
import io.tpersson.ufw.mediator.internal.MediatorImpl
import io.tpersson.ufw.mediator.internal.MediatorInternal
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Provider
import jakarta.inject.Singleton

@Singleton
public class MediatorInternalProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val meterRegistry: MeterRegistry,
    private val injector: Injector
) : Provider<MediatorInternal> {

    private val mediator: MediatorInternal = run {
        val handlers = scanResult.allClasses
            .filter { it.implementsInterface(RequestHandler::class.java) }
            .filter { !it.isAbstract }
            .loadClasses()
            .map { injector.getInstance(it) as RequestHandler<*, *> }
            .toSet()

        val middlewares = scanResult.allClasses
            .filter { it.implementsInterface(Middleware::class.java) }
            .filter { !it.isAbstract }
            .loadClasses()
            .map { it.kotlin }
            .map { injector.getInstance(it.java) as Middleware<*, *> }
            .toSet()

        MediatorImpl(meterRegistry, handlers, middlewares)
    }


    override fun get(): MediatorInternal {
        return mediator
    }
}