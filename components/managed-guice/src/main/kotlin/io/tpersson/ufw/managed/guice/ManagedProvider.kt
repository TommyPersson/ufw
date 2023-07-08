package io.tpersson.ufw.managed.guice

import com.google.inject.Injector
import io.github.classgraph.ClassGraph
import io.tpersson.ufw.managed.Managed
import io.tpersson.ufw.managed.ManagedRunner
import io.tpersson.ufw.managed.guice.internal.ManagedModuleConfig
import jakarta.inject.Inject
import jakarta.inject.Provider

public class ManagedProvider @Inject constructor(
    private val config: ManagedModuleConfig,
    private val injector: Injector
) : Provider<ManagedRunner> {

    private val scanResult = ClassGraph()
        .enableClassInfo()
        .acceptPackages(*config.scanPackages.toTypedArray(), "io.tpersson.ufw")
        .scan()

    private val instances = scanResult.allClasses
        .filter { it.extendsSuperclass(Managed::class.java) }
        .filter { !it.isAbstract }
        .loadClasses()
        .map { injector.getInstance(it) as Managed }
        .toSet()

    override fun get(): ManagedRunner {
        return ManagedRunner(instances)
    }
}