package io.tpersson.ufw.transactionalevents.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import io.tpersson.ufw.transactionalevents.handler.internal.EventHandlersProvider
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class GuiceEventHandlersProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : EventHandlersProvider {

    private val handlers = scanResult.allClasses
        .filter { it.extendsSuperclass(TransactionalEventHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as TransactionalEventHandler }
        .toSet()

    public override fun get(): Set<TransactionalEventHandler> {
        return handlers
    }

    override fun add(handler: TransactionalEventHandler) {
        error("Not supported in Guice-applications")
    }
}
