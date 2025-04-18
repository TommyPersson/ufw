package io.tpersson.ufw.managed.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.managed.component.ManagedComponent
import io.tpersson.ufw.managed.ManagedRunner

public class ManagedGuiceModule : Module {

    override fun configure(binder: Binder) {
        with(binder) {
            bind(ManagedRunner::class.java).toProvider(ManagedRunnerProvider::class.java)
            bind(ManagedComponent::class.java).asEagerSingleton()
        }
    }
}

