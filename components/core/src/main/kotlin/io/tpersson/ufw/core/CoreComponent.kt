package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
            clock: InstantSource = Clock.systemUTC()
        ): CoreComponent {
            // TODO allow extension of object mapper

            return CoreComponent(clock, UFWObjectMapper(defaultObjectMapper))
        }

        public val defaultObjectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().also {
            it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
}

@Suppress("UnusedReceiverParameter")
public val Components.Core: CoreComponent.Companion get() = CoreComponent

