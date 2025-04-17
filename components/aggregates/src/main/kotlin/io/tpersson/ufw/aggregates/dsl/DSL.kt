package io.tpersson.ufw.aggregates.dsl

import io.tpersson.ufw.admin.dsl.admin
import io.tpersson.ufw.admin.dsl.installAdmin
import io.tpersson.ufw.aggregates.AggregatesComponent
import io.tpersson.ufw.core.builder.*
import io.tpersson.ufw.database.dsl.database
import io.tpersson.ufw.database.dsl.installDatabase
import io.tpersson.ufw.durableevents.dsl.durableEvents
import io.tpersson.ufw.durableevents.dsl.installDurableEvents

@UfwDslMarker
public fun UFWBuilder.Root.installAggregates(configure: AggregatesComponentBuilderContext.() -> Unit = {}) {
    installCore()
    installDatabase()
    installDurableEvents()
    installAdmin()

    val ctx = contexts.getOrPut(AggregatesComponent) { AggregatesComponentBuilderContext() }
        .also(configure)

    builders.add(AggregatesComponentBuilder(ctx))
}

public class AggregatesComponentBuilderContext : ComponentBuilderContext<AggregatesComponent>

public class AggregatesComponentBuilder(
    private val context: AggregatesComponentBuilderContext
) : ComponentBuilder<AggregatesComponent> {

    public override fun build(components: ComponentRegistry): AggregatesComponent {
        return AggregatesComponent.create(
            coreComponent = components.core,
            databaseComponent = components.database,
            durableEventsComponent = components.durableEvents,
            adminComponent = components.admin
        )
    }
}

public val ComponentRegistry.aggregates: AggregatesComponent get() = get(AggregatesComponent)