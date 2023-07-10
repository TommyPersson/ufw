package io.tpersson.ufw.managed.guice

import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.managed.Managed
import io.tpersson.ufw.managed.ManagedRunner
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Provider
import jakarta.inject.Singleton

@Singleton
public class ManagedRunnerProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : Provider<ManagedRunner> {

    private val runner: ManagedRunner = run {
        val instances = scanResult.allClasses
            .filter { it.extendsSuperclass(Managed::class.java) }
            .filter { !it.isAbstract }
            .loadClasses()
            .map { injector.getInstance(it) as Managed }
            .toSet()

        ManagedRunner(instances)
    }

    override fun get(): ManagedRunner {
        return runner
    }
}