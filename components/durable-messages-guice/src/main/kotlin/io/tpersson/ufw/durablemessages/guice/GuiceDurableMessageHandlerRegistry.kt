package io.tpersson.ufw.durablemessages.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import io.tpersson.ufw.durablemessages.handler.internal.DurableMessageHandlerRegistry
import io.tpersson.ufw.durablemessages.handler.internal.SimpleDurableMessageHandlersRegistry
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class GuiceDurableMessageHandlerRegistry @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : DurableMessageHandlerRegistry {

    private val handlers = scanResult.allClasses
        .filter { it.implementsInterface(DurableMessageHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as DurableMessageHandler }
        .toSet()

    private val inner = SimpleDurableMessageHandlersRegistry(handlers.toMutableSet())

    public override fun get(): Set<DurableMessageHandler> {
        return inner.get()
    }

    override fun add(handler: DurableMessageHandler) {
        error("Not supported")
    }

    public override val topics: Set<String> get() = inner.topics
}
