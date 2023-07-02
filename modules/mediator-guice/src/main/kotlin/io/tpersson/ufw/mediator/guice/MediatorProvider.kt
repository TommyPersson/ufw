package io.tpersson.ufw.mediator.guice

import com.google.inject.Injector
import io.github.classgraph.ClassGraph
import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.MediatorImpl
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.RequestHandler
import jakarta.inject.Inject
import jakarta.inject.Provider

public class MediatorProvider @Inject constructor(
    private val config: MediatorModuleConfig,
    private val injector: Injector
) : Provider<Mediator> {

    private val scanResult = ClassGraph()
        .enableClassInfo()
        .acceptPackages(*config.scanPackages.toTypedArray(), "io.tpersson.ufw")
        .scan()

    private val handlers = scanResult.allClasses
        .filter { it.implementsInterface(RequestHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as RequestHandler<*, *> }

    private val middlewares = scanResult.allClasses
        .filter { it.implementsInterface(Middleware::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { it.kotlin }
        .map { injector.getInstance(it.java) as Middleware<*, *> }

    private val mediator = MediatorImpl(handlers, middlewares)

    override fun get(): Mediator {
        return mediator
    }
}