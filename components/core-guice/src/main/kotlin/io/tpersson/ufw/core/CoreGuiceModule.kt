package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import com.google.inject.name.Names
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

public class CoreGuiceModule(
    private val configureObjectMapper: ObjectMapper.() -> Unit = {},
    private val scanPackages: Set<String> = emptySet(),
) : Module {
    override fun configure(binder: Binder) {
        val objectMapper = CoreComponent.defaultObjectMapper.also(configureObjectMapper)

        binder.bind(CoreComponent::class.java).asEagerSingleton()
        binder.bind(ObjectMapper::class.java)
            .annotatedWith(Names.named(NamedBindings.ObjectMapper))
            .toInstance(objectMapper)

        OptionalBinder.newOptionalBinder(binder, MeterRegistry::class.java)
            .setDefault()
            .toInstance(SimpleMeterRegistry())

        OptionalBinder.newOptionalBinder(binder, AppInfoProvider::class.java)
            .setDefault()
            .toInstance(AppInfoProvider.simple())

        val finalScanPackages = scanPackages + setOf("io.tpersson.ufw")

        val scanResult = ClassGraph()
            .acceptPackages(*finalScanPackages.toTypedArray())
            .scan()

        binder.bind(ScanResult::class.java)
            .annotatedWith(Names.named(NamedBindings.ScanResult))
            .toInstance(scanResult)
    }
}

