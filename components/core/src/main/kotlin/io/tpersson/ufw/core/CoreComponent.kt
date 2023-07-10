package io.tpersson.ufw.core

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock
import java.time.InstantSource

@Singleton
public class CoreComponent @Inject private constructor(
    public val clock: InstantSource,
    public val objectMapper: UFWObjectMapper,
) {
    public companion object {
        public fun create(
            clock: InstantSource = Clock.systemUTC(),
            objectMapper: UFWObjectMapper = UFWObjectMapper.default,
        ): CoreComponent {
            return CoreComponent(clock, objectMapper)
        }
    }
}

@Suppress("UnusedReceiverParameter")
public val Components.Core: CoreComponent.Companion get() = CoreComponent

