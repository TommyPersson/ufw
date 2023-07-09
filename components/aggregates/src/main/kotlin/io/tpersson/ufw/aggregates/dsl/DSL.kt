package io.tpersson.ufw.aggregates.dsl

import io.tpersson.ufw.core.dsl.UFWBuilder
import io.tpersson.ufw.core.dsl.UFWRegistry
import io.tpersson.ufw.core.dsl.UfwDslMarker
import io.tpersson.ufw.core.dsl.core
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.aggregates.AggregatesComponent

@UfwDslMarker
public fun UFWBuilder.RootBuilder.aggregates(builder: AggregatesComponentBuilder.() -> Unit) {
    components["Aggregates"] = AggregatesComponentBuilder(UFWRegistry(components)).also(builder).build()
}

@UfwDslMarker
public class AggregatesComponentBuilder(public val components: UFWRegistry) {
    public fun build(): AggregatesComponent {
        return AggregatesComponent.create(components.core, components.database)
    }
}

public val UFWRegistry.aggregates: AggregatesComponent get() = _components["Aggregates"] as AggregatesComponent