package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.OptionalBinder
import com.google.inject.name.Names
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import java.util.*

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

        OptionalBinder.newOptionalBinder(binder, OpenTelemetry::class.java)

        val finalScanPackages = scanPackages + setOf("io.tpersson.ufw")

        val scanResult = ClassGraph()
            .acceptPackages(*finalScanPackages.toTypedArray())
            .scan()

        binder.bind(ScanResult::class.java)
            .annotatedWith(Names.named(NamedBindings.ScanResult))
            .toInstance(scanResult)

        binder.bind(object : TypeLiteral<Optional<Meter>>() {})
            .annotatedWith(Names.named(NamedBindings.Meter))
            .toProvider(UFWMeterProvider::class.java)
    }
}

