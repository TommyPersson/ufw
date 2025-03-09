package io.tpersson.ufw.jobqueue.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.jobqueue.DurableJobHandler
import io.tpersson.ufw.jobqueue.internal.DurableJobHandlersProvider
import jakarta.inject.Inject
import jakarta.inject.Named

public class GuiceDurableJobHandlersProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : DurableJobHandlersProvider {

    private val handlers = scanResult.allClasses
        .filter { it.implementsInterface(DurableJobHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as DurableJobHandler<*> }

    public override fun get(): Set<DurableJobHandler<*>> {
        return handlers.toSet()
    }
}