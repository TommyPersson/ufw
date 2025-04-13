package io.tpersson.ufw.core.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tpersson.ufw.core.AppInfo
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.SimpleAppInfoProvider
import io.tpersson.ufw.core.configuration.ConfigProvider
import java.time.Clock


@UfwDslMarker
public fun UFWBuilder.RootBuilder.core(builder: CoreComponentBuilder.() -> Unit) {
    components["core"] = CoreComponentBuilder().also(builder).build()
}

@UfwDslMarker
public class CoreComponentBuilder {
    public var clock: Clock = Clock.systemDefaultZone()
    public var meterRegistry: MeterRegistry = SimpleMeterRegistry()
    public var appInfoProvider: AppInfoProvider = SimpleAppInfoProvider(AppInfo("unknown", "unknown", "unknown"))
    // TODO naming ...
    public var configProviderFactory: () -> ConfigProvider = { ConfigProvider.default() }

    internal var protoObjectMapper = CoreComponent.defaultObjectMapper

    public fun build(): CoreComponent {
        return CoreComponent.create(
            clock,
            meterRegistry,
            appInfoProvider,
            protoObjectMapper,
            configProviderFactory
        )
    }
}

@UfwDslMarker
public fun CoreComponentBuilder.objectMapper(builder: ObjectMapper.() -> Unit) {
    val objectMapper = CoreComponent.defaultObjectMapper
    builder(objectMapper)
    protoObjectMapper = objectMapper
}

public val UFWRegistry.core: CoreComponent get() = _components["core"] as CoreComponent