package io.tpersson.ufw.core.component

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tpersson.ufw.core.AppInfo
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.SimpleAppInfoProvider
import io.tpersson.ufw.core.components.ComponentBuilder
import io.tpersson.ufw.core.components.ComponentBuilderContext
import io.tpersson.ufw.core.components.ComponentRegistryInternal
import io.tpersson.ufw.core.builder.UFWBuilder
import io.tpersson.ufw.core.builder.UfwDslMarker
import io.tpersson.ufw.core.configuration.ConfigProvider
import java.time.Clock
import java.util.UUID


@UfwDslMarker
public fun UFWBuilder.Root.installCore(configure: CoreComponentBuilderContext.() -> Unit = {}) {
    val ctx = contexts.getOrPut(CoreComponent) { CoreComponentBuilderContext() }
        .also(configure)

    if (ctx.configProvider == null) {
        ctx.configProvider = ConfigProvider.default()
    }

    builders.add(CoreComponentBuilder(ctx))
}


public class CoreComponentBuilderContext : ComponentBuilderContext<CoreComponent> {
    public var clock: Clock = Clock.systemDefaultZone()
    public var meterRegistry: MeterRegistry = SimpleMeterRegistry()
    public var appInfoProvider: AppInfoProvider = SimpleAppInfoProvider(AppInfo("unknown", "unknown", "unknown", UUID.randomUUID().toString()))
    public var objectMapper: ObjectMapper = CoreComponent.defaultObjectMapper
    public var configProvider: ConfigProvider? = null
}

public class CoreComponentBuilder(
    private val context: CoreComponentBuilderContext,
) : ComponentBuilder<CoreComponent> {

    public override fun build(components: ComponentRegistryInternal): CoreComponent {
        return CoreComponent(
            context.clock,
            context.objectMapper,
            context.meterRegistry,
            context.appInfoProvider,
            context.configProvider ?: error("ConfigProvider not set!"),
        )
    }
}