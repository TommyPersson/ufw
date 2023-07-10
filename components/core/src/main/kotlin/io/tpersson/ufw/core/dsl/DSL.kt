package io.tpersson.ufw.core.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.OpenTelemetry
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.UFWObjectMapper
import java.time.Clock
import java.time.InstantSource


@UfwDslMarker
public fun UFWBuilder.RootBuilder.core(builder: CoreComponentBuilder.() -> Unit) {
    components["core"] = CoreComponentBuilder().also(builder).build()
}

@UfwDslMarker
public class CoreComponentBuilder {
    public var clock: InstantSource = Clock.systemUTC()
    public var openTelemetry: OpenTelemetry? = null

    internal var ufwObjectMapper = UFWObjectMapper.default

    public fun build(): CoreComponent {
        return CoreComponent.create(clock, openTelemetry, ufwObjectMapper)
    }
}

@UfwDslMarker
public fun CoreComponentBuilder.objectMapper(builder: ObjectMapper.() -> Unit) {
    val objectMapper = UFWObjectMapper.default.objectMapper
    builder(objectMapper)
    ufwObjectMapper = UFWObjectMapper(objectMapper)
}

public val UFWRegistry.core: CoreComponent get() = _components["core"] as CoreComponent