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
public fun UFWBuilder.RootBuilder.installCore(builder: CoreComponentBuilderContext.() -> Unit = {}) {
    val ctx = contexts.getOrPut(CoreComponent) { CoreComponentBuilderContext() }.also(builder)
    builder(ctx)

    builders.add(CoreComponentBuilder(ctx))
}


public class CoreComponentBuilderContext : ComponentBuilderContext<CoreComponent> {
    public var clock: Clock = Clock.systemDefaultZone()
    public var meterRegistry: MeterRegistry = SimpleMeterRegistry()
    public var appInfoProvider: AppInfoProvider = SimpleAppInfoProvider(AppInfo("unknown", "unknown", "unknown"))
    public var objectMapper: ObjectMapper = CoreComponent.defaultObjectMapper

    // TODO naming ...
    public var configProviderFactory: () -> ConfigProvider = { ConfigProvider.default() }
}

public class CoreComponentBuilder(
    private val context: CoreComponentBuilderContext,
) : ComponentBuilder<CoreComponent> {

    public override fun build(components: UFWComponentRegistry): CoreComponent {
        return CoreComponent.create(
            this.context.clock,
            this.context.meterRegistry,
            this.context.appInfoProvider,
            this.context.objectMapper,
            this.context.configProviderFactory
        )
    }
}

public val UFWComponentRegistry.core: CoreComponent get() = get(CoreComponent)