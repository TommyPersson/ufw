package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.builders.ComponentKey
import io.tpersson.ufw.core.builders.Component
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Clock

@Singleton
public class CoreComponent @Inject private constructor(
    public val clock: Clock,
    @Named(NamedBindings.ObjectMapper) public val objectMapper: ObjectMapper,
    public val meterRegistry: MeterRegistry,
    public val appInfoProvider: AppInfoProvider,
    public val configProvider: ConfigProvider,
) : Component<CoreComponent> {

    public companion object : ComponentKey<CoreComponent> {

        public fun create(
            clock: Clock = Clock.systemDefaultZone(),
            meterRegistry: MeterRegistry = SimpleMeterRegistry(),
            appInfoProvider: AppInfoProvider = AppInfoProvider.simple(),
            objectMapper: ObjectMapper = defaultObjectMapper,
            configProviderFactory: () -> ConfigProvider = { ConfigProvider.default() },
        ): CoreComponent {
            return CoreComponent(clock, objectMapper, meterRegistry, appInfoProvider, configProviderFactory())
        }

        public val defaultObjectMapper: ObjectMapper =
            jacksonObjectMapper().findAndRegisterModules().also {
                it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                it.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            }
    }
}
