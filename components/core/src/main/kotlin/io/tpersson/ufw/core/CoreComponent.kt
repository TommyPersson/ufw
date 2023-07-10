package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.opentelemetry.api.OpenTelemetry
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Clock
import java.time.InstantSource
import java.util.*

@Singleton
public class CoreComponent @Inject private constructor(
    public val clock: InstantSource,
    public val openTelemetry: Optional<OpenTelemetry>,
    @Named(NamedBindings.ObjectMapper) public val objectMapper: ObjectMapper,
) {
    public companion object {
        public fun create(
            clock: InstantSource = Clock.systemUTC(),
            openTelemetry: OpenTelemetry? = null,
            objectMapper: ObjectMapper = defaultObjectMapper,
        ): CoreComponent {
            return CoreComponent(clock, Optional.ofNullable(openTelemetry), objectMapper)
        }

        public val defaultObjectMapper: ObjectMapper =
            jacksonObjectMapper().findAndRegisterModules().also {
                it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }

    }
}
