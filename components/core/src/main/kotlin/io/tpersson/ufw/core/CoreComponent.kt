package io.tpersson.ufw.core

import io.opentelemetry.api.OpenTelemetry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock
import java.time.InstantSource

@Singleton
public class CoreComponent @Inject private constructor(
    public val clock: InstantSource,
    public val openTelemetry: OpenTelemetry?,
    public val objectMapper: UFWObjectMapper,
) {
    public companion object {
        public fun create(
            clock: InstantSource = Clock.systemUTC(),
            openTelemetry: OpenTelemetry? = null,
            objectMapper: UFWObjectMapper = UFWObjectMapper.default,
        ): CoreComponent {
            return CoreComponent(clock, openTelemetry, objectMapper)
        }
    }
}

@Suppress("UnusedReceiverParameter")
public val Components.Core: CoreComponent.Companion get() = CoreComponent

