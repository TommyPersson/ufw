package io.tpersson.ufw.core.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tpersson.ufw.core.CoreComponent
import java.time.Clock


@UfwDslMarker
public fun UFWBuilder.RootBuilder.core(builder: CoreComponentBuilder.() -> Unit) {
    components["core"] = CoreComponentBuilder().also(builder).build()
}

@UfwDslMarker
public class CoreComponentBuilder {
    public var clock: Clock = Clock.systemDefaultZone()
    public var meterRegistry: MeterRegistry = SimpleMeterRegistry()

    internal var protoObjectMapper = CoreComponent.defaultObjectMapper

    public fun build(): CoreComponent {
        return CoreComponent.create(clock, meterRegistry, protoObjectMapper)
    }
}

@UfwDslMarker
public fun CoreComponentBuilder.objectMapper(builder: ObjectMapper.() -> Unit) {
    val objectMapper = CoreComponent.defaultObjectMapper
    builder(objectMapper)
    protoObjectMapper = objectMapper
}

public val UFWRegistry.core: CoreComponent get() = _components["core"] as CoreComponent