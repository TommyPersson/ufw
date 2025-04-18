package io.tpersson.ufw.aggregates.builder

import io.tpersson.ufw.admin.builder.admin
import io.tpersson.ufw.admin.builder.installAdmin
import io.tpersson.ufw.aggregates.AggregatesComponent
import io.tpersson.ufw.core.builder.core
import io.tpersson.ufw.core.builder.installCore
import io.tpersson.ufw.core.builders.*
import io.tpersson.ufw.database.builder.database
import io.tpersson.ufw.database.builder.installDatabase
import io.tpersson.ufw.durableevents.builder.durableEvents
import io.tpersson.ufw.durableevents.builder.installDurableEvents

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