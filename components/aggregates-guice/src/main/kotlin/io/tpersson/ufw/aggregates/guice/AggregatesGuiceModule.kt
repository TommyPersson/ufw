package io.tpersson.ufw.aggregates.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.aggregates.AggregatesComponent

public class AggregatesGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(AggregatesComponent::class.java).asEagerSingleton()
        }
    }
}