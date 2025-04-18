package io.tpersson.ufw.core.component

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.components.ComponentKey
import io.tpersson.ufw.core.components.Component
import io.tpersson.ufw.core.components.ComponentRegistry
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Clock

@Singleton
public class CoreComponent @Inject public constructor(
    public val clock: Clock,
    @Named(NamedBindings.ObjectMapper) public val objectMapper: ObjectMapper,
    public val meterRegistry: MeterRegistry,
    public val appInfoProvider: AppInfoProvider,
    public val configProvider: ConfigProvider,
) : Component<CoreComponent> {

    public companion object : ComponentKey<CoreComponent> {

        public val defaultObjectMapper: ObjectMapper =
            jacksonObjectMapper().findAndRegisterModules().also {
                it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                it.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            }
    }
}

public val ComponentRegistry.core: CoreComponent get() = get(CoreComponent)