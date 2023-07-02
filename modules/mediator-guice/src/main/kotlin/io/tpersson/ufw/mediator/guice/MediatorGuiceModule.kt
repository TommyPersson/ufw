package io.tpersson.ufw.mediator.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.mediator.Mediator

public class MediatorGuiceModule(
    private val config: MediatorModuleConfig
) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(MediatorModuleConfig::class.java).toInstance(config)
            bind(Mediator::class.java).toProvider(MediatorProvider::class.java)
        }
    }

}
