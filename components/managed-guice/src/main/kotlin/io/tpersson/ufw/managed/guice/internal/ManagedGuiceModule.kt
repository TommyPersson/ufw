package io.tpersson.ufw.managed.guice.internal

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.managed.ManagedRunner
import io.tpersson.ufw.managed.guice.ManagedRunnerProvider

public class ManagedGuiceModule(
    private val scanPackages: List<String>,
) : Module {

    override fun configure(binder: Binder) {
        with(binder) {
            val config = ManagedModuleConfig(scanPackages)

            bind(ManagedModuleConfig::class.java).toInstance(config)
            bind(ManagedRunner::class.java).toProvider(ManagedRunnerProvider::class.java).asEagerSingleton()
        }
    }
}

