package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import com.google.inject.name.Names
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import io.opentelemetry.api.OpenTelemetry

public class CoreGuiceModule(
    private val configureObjectMapper: ObjectMapper.() -> Unit = {},
    private val scanPackages: Set<String> = emptySet(),
) : Module {
    override fun configure(binder: Binder) {
        val objectMapper = UFWObjectMapper.default.objectMapper.also(configureObjectMapper)

        binder.bind(CoreComponent::class.java).asEagerSingleton()
        binder.bind(UFWObjectMapper::class.java).toInstance(UFWObjectMapper(objectMapper))

        OptionalBinder.newOptionalBinder(binder, OpenTelemetry::class.java)

        val finalScanPackages = scanPackages + setOf("io.tpersson.ufw")

        val scanResult = ClassGraph()
            .acceptPackages(*finalScanPackages.toTypedArray())
            .scan()

        binder.bind(ScanResult::class.java)
            .annotatedWith(Names.named("UFW_ScanResult"))
            .toInstance(scanResult)
    }
}

