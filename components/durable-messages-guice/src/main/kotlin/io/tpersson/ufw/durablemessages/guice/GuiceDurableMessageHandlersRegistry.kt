package io.tpersson.ufw.durablemessages.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import io.tpersson.ufw.durablemessages.handler.internal.DurableMessageHandlersRegistry
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class GuiceDurableMessageHandlersRegistry @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : DurableMessageHandlersRegistry {

    private val handlers = scanResult.allClasses
        .filter { it.implementsInterface(DurableMessageHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as DurableMessageHandler }
        .toSet()

    public override fun get(): Set<DurableMessageHandler> {
        return handlers
    }

    override fun add(handler: DurableMessageHandler) {
        error("Not supported")
    }
}
