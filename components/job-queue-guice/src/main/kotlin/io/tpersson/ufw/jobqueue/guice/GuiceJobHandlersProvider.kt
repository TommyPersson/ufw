package io.tpersson.ufw.jobqueue.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.jobqueue.JobHandler
import io.tpersson.ufw.jobqueue.internal.JobHandlersProvider
import jakarta.inject.Inject
import jakarta.inject.Named

public class GuiceJobHandlersProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : JobHandlersProvider {

    private val handlers = scanResult.allClasses
        .filter { it.extendsSuperclass(JobHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as JobHandler<*> }

    public override fun get(): Set<JobHandler<*>> {
        return handlers.toSet()
    }
}
