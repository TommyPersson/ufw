package io.tpersson.ufw.jobqueue.guice

import com.google.inject.Injector
import io.github.classgraph.ClassGraph
import io.tpersson.ufw.jobqueue.JobHandler
import io.tpersson.ufw.jobqueue.JobHandlersProvider
import io.tpersson.ufw.jobqueue.JobQueueModuleConfig
import jakarta.inject.Inject

public class GuiceJobHandlersProvider @Inject constructor(
    private val config: JobQueueModuleConfig,
    private val injector: Injector
) : JobHandlersProvider {
    private val scanResult = ClassGraph()
        .enableClassInfo()
        .acceptPackages(*config.scanPackages.toTypedArray(), "io.tpersson.ufw")
        .scan()

    private val handlers = scanResult.allClasses
        .filter { it.implementsInterface(JobHandler::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as JobHandler<*> }

    public override fun get(): Set<JobHandler<*>> {
        return handlers.toSet()
    }
}
