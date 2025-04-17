package io.tpersson.ufw.durableevents.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.durableevents.handler.internal.DurableEventHandlersProvider
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class GuiceDurableEventHandlersProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : DurableEventHandlersProvider {

    private val handlers = scanResult.allClasses
        .filter { it.implementsInterface(DurableEventHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as DurableEventHandler }
        .toSet()

    public override fun get(): Set<DurableEventHandler> {
        return handlers
    }

    override fun add(handler: DurableEventHandler) {
        error("Not supported")
    }
}
