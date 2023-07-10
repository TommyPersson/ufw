package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Binder
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.multibindings.OptionalBinder
import com.google.inject.util.Providers
import io.opentelemetry.api.OpenTelemetry

public class CoreGuiceModule(
    private val configureObjectMapper: ObjectMapper.() -> Unit = {}
) : Module {
    override fun configure(binder: Binder) {
        val objectMapper = UFWObjectMapper.default.objectMapper.also(configureObjectMapper)

        binder.bind(CoreComponent::class.java)
        binder.bind(UFWObjectMapper::class.java).toInstance(UFWObjectMapper(objectMapper))

        OptionalBinder.newOptionalBinder(binder, Key.get(OpenTelemetry::class.java))
            .setDefault()
            .toProvider(Providers.of(null))

    }
}

