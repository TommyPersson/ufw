package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Clock
import java.time.InstantSource
import java.util.*

@Singleton
public class CoreComponent @Inject private constructor(
    public val clock: InstantSource,
    @Named(NamedBindings.ObjectMapper) public val objectMapper: ObjectMapper,
    public val meterRegistry: MeterRegistry,
) {
    public companion object {
        public fun create(
            clock: InstantSource = Clock.systemUTC(),
            meterRegistry: MeterRegistry = SimpleMeterRegistry(),
            objectMapper: ObjectMapper = defaultObjectMapper,
        ): CoreComponent {
            return CoreComponent(clock, objectMapper, meterRegistry)
        }

        public val defaultObjectMapper: ObjectMapper =
            jacksonObjectMapper().findAndRegisterModules().also {
                it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                it.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            }

    }
}
